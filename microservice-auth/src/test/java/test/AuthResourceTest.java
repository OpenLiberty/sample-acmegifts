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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Test;

public class AuthResourceTest {

  /**
   * This is the URL that the Liberty auth server is listening on. We'll use it to call JAX-RS
   * services in our tests.
   */
  private static final String authServiceURL = System.getProperty("liberty.test.auth.service.url");

  /**
   * Tests the JWT we get back from the auth service is valid. We test the JWT to make sure it was
   * signed correctly.
   *
   * <p>We do not validate other things, like the issued at time, expired time, etc.
   *
   * <p>The test case has access to the keystore that the server should have used to sign the JWT.
   */
  @Test
  public void testLoginJwtValidity() throws Exception {
    // Get the JWT from the auth service.
    Response response = processRequest(authServiceURL, "GET", null, null);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");

    // Open the keystore that the server should have used to sign the JWT.
    KeyStore ks = KeyStore.getInstance("JCEKS");
    InputStream ksStream = this.getClass().getResourceAsStream("/keystore.jceks");
    char[] password = new String("secret").toCharArray();
    ks.load(ksStream, password);
    java.security.cert.Certificate cert = ks.getCertificate("default");
    PublicKey publicKey = cert.getPublicKey();

    // Make sure it's valid.  Use the server's public key to check.
    new JWTVerifier().validateJWT(authHeader, publicKey);
  }

  public static Response processRequest(
      String url, String method, String payload, String authHeader) {
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(url);
    Builder builder = target.request();
    if (payload != null) {
      builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }
    if (authHeader != null) {
      builder.header(HttpHeaders.AUTHORIZATION, authHeader);
    }
    return (payload != null)
        ? builder.build(method, Entity.json(payload)).invoke()
        : builder.build(method).invoke();
  }
}
