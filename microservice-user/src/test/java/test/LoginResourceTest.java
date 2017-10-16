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
import static org.junit.Assert.assertTrue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests the endpoints on the login service. */
public class LoginResourceTest {

  private static MongoClient mongo;
  private static DB database;
  private static final int mongoPort = Integer.parseInt(System.getProperty("mongo.test.port"));
  private static final String mongoHostname = System.getProperty("mongo.test.hostname");
  private static final String userServiceURL = System.getProperty("liberty.test.user.service.url");
  private static final String userServiceLoginURL =
      System.getProperty("liberty.test.user.service.login.url");
  private static final String libertyPort = System.getProperty("liberty.test.port");
  private static final String libertyHostname =
      System.getProperty("liberty.test.user.service.hostname");

  @BeforeClass
  public static void setup() throws Exception {
    // Open a connection to the Mongo database before the tests start.
    mongo = new MongoClient(mongoHostname, mongoPort);
    database = mongo.getDB("gifts-user");
  }

  @AfterClass
  public static void cleanup() {
    // Clean up the mess we made in the database.
    database.dropDatabase();
    mongo.close();
  }

  @After
  public void postTestProcessing() {
    // After each test, drop the user database.
    database.getCollection(User.DB_COLLECTION_NAME).drop();
  }

  /** Tests the login function. */
  @Test
  public void testLogin() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(null, "Niels", "Bohr", "nBohr", "@nBohr", "nBohrWishListLink", "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(User.JSON_KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Login user.
    JsonObjectBuilder loginPayload = Json.createObjectBuilder();
    loginPayload.add(User.JSON_KEY_USER_NAME, user.userName);
    loginPayload.add(User.JSON_KEY_USER_PASSWORD, user.password);

    response =
        processRequest(
            userServiceLoginURL, "POST", loginPayload.build().toString(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    JsonObject postResponseJson = toJsonObj(response.readEntity(String.class));
    String postDbId = postResponseJson.getString(User.JSON_KEY_USER_ID);

    // Validate the user id.
    assertTrue(
        "The database user ids for the specified user did not match.",
        dbUser.getString(User.DB_ID).equals(postDbId));

    // Test 2: Test login for a non-existing user.
    loginPayload.add(User.JSON_KEY_USER_NAME, "nonExistentUser");
    loginPayload.add(User.JSON_KEY_USER_PASSWORD, "Something");
    response =
        processRequest(
            userServiceLoginURL, "POST", loginPayload.build().toString(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.BAD_REQUEST.getStatusCode() + ".",
        Status.BAD_REQUEST.getStatusCode(),
        response.getStatus());
  }

  /** Tests the login function with a JWT in the wrong group. We should not be able to log in. */
  @Test
  public void testLoginWrongJwtGroup() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(null, "Niels", "Bohr", "nBohr", "@nBohr", "nBohrWishListLink", "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(User.JSON_KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Login user.
    JsonObjectBuilder loginPayload = Json.createObjectBuilder();
    loginPayload.add(User.JSON_KEY_USER_NAME, user.userName);
    loginPayload.add(User.JSON_KEY_USER_PASSWORD, user.password);

    // Use the JWT that we got back from the logged-in use to log in a new user.
    // This should not succeed.
    response =
        processRequest(userServiceLoginURL, "POST", loginPayload.build().toString(), authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.UNAUTHORIZED.getStatusCode() + ".",
        Status.UNAUTHORIZED.getStatusCode(),
        response.getStatus());
  }

  /** Tests login with the wrong password. */
  @Test
  public void testLoginWrongPassword() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(null, "Niels", "Bohr", "nBohr", "@nBohr", "nBohrWishListLink", "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(User.JSON_KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Login the user with an incorrect password.  This should fail.
    JsonObjectBuilder loginPayload = Json.createObjectBuilder();
    loginPayload.add(User.JSON_KEY_USER_NAME, user.userName);
    loginPayload.add(User.JSON_KEY_USER_PASSWORD, user.password + "X");

    response =
        processRequest(
            userServiceLoginURL, "POST", loginPayload.build().toString(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.UNAUTHORIZED.getStatusCode() + ".",
        Status.UNAUTHORIZED.getStatusCode(),
        response.getStatus());
  }

  /** Tests login via the non-SSL port. The connection should be denied or forwarded. */
  @Test
  public void testLoginNonSsl() throws Exception {

    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(null, "Niels", "Bohr", "nBohr", "@nBohr", "nBohrWishListLink", "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(User.JSON_KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Login the user on non-ssl port.  We should be redirected to
    // the SSL port.
    String postUrl = "http://" + libertyHostname + ":" + libertyPort + "/logins";
    JsonObjectBuilder loginPayload = Json.createObjectBuilder();
    loginPayload.add(User.JSON_KEY_USER_NAME, user.userName);
    loginPayload.add(User.JSON_KEY_USER_PASSWORD, user.password);

    response = processRequest(postUrl, "POST", loginPayload.build().toString(), authHeader);
    assertEquals("HTTP response code should have been 302.", 302, response.getStatus());
  }

  public static Response processRequest(
      String url, String method, String payload, String authHeader) {
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

  public JsonObject toJsonObj(String json) {
    try (JsonReader jReader = Json.createReader(new StringReader(json))) {
      return jReader.readObject();
    }
  }
}
