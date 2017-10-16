// ******************************************************************************
//  Copyright (c) 2017 IBM Corporation and others.
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/epl-v10.html
//
//  Contributors:
//  IBM Corporation - initial API and implementation
// ******************************************************************************
package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.junit.Assert;

public class JWTVerifier {

  /** The algorithm used to sign the JWT. */
  private static final String JWT_ALGORITHM = "SHA256withRSA";

  /**
   * The issuer of the JWT. This must match the issuer that the liberty server expects, defined in
   * server.xml.
   */
  private static final String JWT_ISSUER = System.getProperty("jwt.issuer", "http://wasdev.net");

  /** The hostname we'll use in our tests. The hostname of the user service. */
  private static final String libertyHostname =
      System.getProperty("liberty.test.user.service.hostname");

  /** The SSL port we'll use in our tests. The ssl port of the user service. */
  private static final String libertySslPort = System.getProperty("liberty.user.service.ssl.port");

  /**
   * Validate that the response contains an authorization header, and that the JWT inside can be
   * decoded using the public key of the server.
   *
   * @param authHeader The authorization header received from the microservice.
   */
  public void validateJWT(String authHeader) {
    try {
      validateJWT(authHeader, getPublicKey());
    } catch (Base64Exception be) {
      Assert.fail("Exception decoding JWT signature: " + be.toString());
    } catch (Throwable t) {
      System.out.println(t.toString());
      t.printStackTrace(System.out);
      Assert.fail("Exception validating JWT signature: " + t.toString());
    }
  }

  public void validateJWT(String authHeader, PublicKey publicKey) {
    assertNotNull("Authorization header was not present in response", authHeader);
    assertTrue("Authorization header does not contain a bearer", authHeader.startsWith("Bearer "));

    StringTokenizer st = new StringTokenizer(authHeader.substring(7), ".");
    assertTrue("JWT does not contain three parts", st.countTokens() == 3);

    String jwtHeaderEnc = st.nextToken();
    String jwtClaimsEnc = st.nextToken();
    String jwtSigEnc = st.nextToken();

    try {
      // Decode the signature we got from the server
      byte[] jwtExpectedSig = Base64Utility.decode(jwtSigEnc, true);

      // Validate the signature.
      Signature sig = Signature.getInstance(JWT_ALGORITHM);
      sig.initVerify(publicKey);
      sig.update(new String(jwtHeaderEnc + "." + jwtClaimsEnc).getBytes());
      assertTrue("JWT expected and actual signatures don't match", sig.verify(jwtExpectedSig));
    } catch (Base64Exception be) {
      Assert.fail("Exception decoding JWT signature: " + be.toString());
    } catch (Throwable t) {
      System.out.println(t.toString());
      t.printStackTrace(System.out);
      Assert.fail("Exception validating JWT signature: " + t.toString());
    }
  }

  /**
   * Make a microprofile-compliant JWT with the correct secret key.
   *
   * @return A base 64 encoded JWT.
   */
  public String createJWT(String username) throws GeneralSecurityException, IOException {
    Set<String> groups = new HashSet<String>();
    groups.add("users");
    return createJWT(username, groups);
  }

  public String createJWT(String username, Set<String> groups)
      throws GeneralSecurityException, IOException {
    // Create and Base64 encode the header portion of the JWT
    JsonObject headerObj =
        Json.createObjectBuilder()
            .add("alg", "RS256") /* Algorithm used */
            .add("typ", "JWT") /* Type of token */
            // .add("kid", "default") /* Hint about which key to use to sign, but the signature is
            // invalid when I include this. */
            .build();
    String headerEnc = Base64Utility.encode(headerObj.toString().getBytes(), true);

    // Create and Base64 encode the claims portion of the JWT
    JsonObject claimsObj =
        Json.createObjectBuilder()
            .add("exp", (System.currentTimeMillis() / 1000) + 300) /* Expire time */
            .add("iat", (System.currentTimeMillis() / 1000)) /* Issued time */
            .add("aud", "acmeGifts") /* Audience */
            .add("jti", Long.toHexString(System.nanoTime())) /* Unique value */
            .add("sub", username) /* Subject */
            .add("upn", username) /* Subject again */
            .add("iss", JWT_ISSUER) /* Issuer */
            .add("groups", getGroupArray(groups)) /* Group list */
            .build();
    String claimsEnc = Base64Utility.encode(claimsObj.toString().getBytes(), true);
    String headerClaimsEnc = headerEnc + "." + claimsEnc;

    // Open the keystore that the server will use to validate the JWT
    KeyStore ks = KeyStore.getInstance("JCEKS");
    InputStream ksStream = this.getClass().getResourceAsStream("/keystore.jceks");
    char[] password = new String("secret").toCharArray();
    ks.load(ksStream, password);

    // Get the private key to use to sign the JWT.  Normally we would not do this but
    // we are pretending to be the user service here.
    KeyStore.ProtectionParameter keyPassword = new KeyStore.PasswordProtection(password);
    KeyStore.PrivateKeyEntry privateKeyEntry =
        (KeyStore.PrivateKeyEntry) ks.getEntry("default", keyPassword);
    PrivateKey privateKey = privateKeyEntry.getPrivateKey();

    // Sign the JWT
    Signature sig = Signature.getInstance(JWT_ALGORITHM);
    sig.initSign(privateKey);
    sig.update(headerClaimsEnc.getBytes());
    String sigEnc = Base64Utility.encode(sig.sign(), true);

    // Lets just check......
    String jwtEnc = headerClaimsEnc + "." + sigEnc;
    java.security.cert.Certificate cert = ks.getCertificate("default");
    PublicKey publicKey = cert.getPublicKey();
    validateJWT("Bearer " + jwtEnc, publicKey);

    // Return the complete JWT (header, claims, signature).
    return jwtEnc;
  }

  /** Create a groups array to put in the JWT. */
  private JsonArray getGroupArray(Set<String> groups) {
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

    if (groups != null) {
      for (String group : groups) {
        arrayBuilder.add(group);
      }
    }

    return arrayBuilder.build();
  }

  /**
   * Get the public key that is used to verify the JWT from the user service. We assume the key is
   * an RSA key.
   *
   * @throws NoSuchAlgorithmException
   */
  private PublicKey getPublicKey()
      throws Base64Exception, InvalidKeySpecException, NoSuchAlgorithmException {
    String url =
        "https://" + libertyHostname + ":" + libertySslPort + "/jwt/ibm/api/jwtUserBuilder/jwk";
    Response response = processRequest(url, "GET", null, null);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Liberty returns the keys in an array.  We'll grab the first one (there
    // should only be one).
    JsonObject jwkResponse = toJsonObj(response.readEntity(String.class));
    JsonArray jwkArray = jwkResponse.getJsonArray("keys");
    JsonObject jwk = jwkArray.getJsonObject(0);
    BigInteger modulus = new BigInteger(1, Base64Utility.decode(jwk.getString("n"), true));
    BigInteger publicExponent = new BigInteger(1, Base64Utility.decode(jwk.getString("e"), true));
    return KeyFactory.getInstance("RSA")
        .generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
  }

  public JsonObject toJsonObj(String json) {
    try (JsonReader jReader = Json.createReader(new StringReader(json))) {
      return jReader.readObject();
    }
  }

  private Response processRequest(String url, String method, String payload, String authHeader) {
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(url);
    Builder builder = target.request();
    builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    if (authHeader != null) {
      builder.header(HttpHeaders.AUTHORIZATION, authHeader);
    }
    return (payload != null)
        ? builder.build(method, Entity.json(payload)).invoke()
        : builder.build(method).invoke();
  }
}
