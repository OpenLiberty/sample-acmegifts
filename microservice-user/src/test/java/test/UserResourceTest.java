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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import javax.json.Json;
import javax.json.JsonArray;
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
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** User Resource tests. */
public class UserResourceTest {
  public static final String KEY_USER_ID = "id";
  public static final String DB_USERS_COLLECTION_NAME = "users";
  public static final String USER_FIRST_NAME_KEY = "firstName";
  public static final String USER_LAST_NAME_KEY = "lastName";
  public static final String USER_USER_NAME_KEY = "userName";
  public static final String USER_WISH_LIST_LINK_KEY = "wishListLink";

  private static MongoClient mongo;
  private static DB database;

  private static final String userServiceURL = System.getProperty("liberty.test.user.service.url");
  private static final int mongoPort = Integer.parseInt(System.getProperty("mongo.test.port"));
  private static final String mongoHostname = System.getProperty("mongo.test.hostname");

  @BeforeClass
  public static void setup() throws Exception {
    // Connect to the database before starting tests.
    mongo = new MongoClient(mongoHostname, mongoPort);
    database = mongo.getDB("gifts-user");
  }

  @AfterClass
  public static void cleanup() {
    database.dropDatabase();
    mongo.close();
  }

  @After
  public void postTestProcessing() {
    // Cleanup the database after each test.
    database.getCollection(DB_USERS_COLLECTION_NAME).drop();
  }

  /** Tests the create user function. */
  @Test
  public void testCreateUser() throws Exception {
    // Test1: Add user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user1 =
        new User(
            null, "Isaac", "Newton", "inewton", "@inewton", "inewtonWishListLink", "mypassword");
    Response response = processRequest(userServiceURL, "POST", user1.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(KEY_USER_ID);
    user1.setId(dbId);

    // Validate user.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User inewton was NOT found in database.", dbUser != null);
    assertTrue("User inewton does not contain expected data.", user1.isEqual(dbUser));

    // Test2: Try adding another user with the same userName.  This should fail.
    User user2 =
        new User(
            null,
            "Ivan",
            "Newton",
            "inewton",
            "@ivannewton",
            "ivannewtonWishListLink",
            "myPassword");
    response = processRequest(userServiceURL, "POST", user2.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.BAD_REQUEST.getStatusCode() + ".",
        Status.BAD_REQUEST.getStatusCode(),
        response.getStatus());
  }

  /** Tests the create user function with a JWT that has an incorrect group. */
  @Test
  public void testCreateUserInvalidJwtGroup() throws Exception {
    // Test1: Add user.  Use a JWT that is not in the login group.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("users")));
    User user1 =
        new User(
            null, "Isaac", "Newton", "inewton", "@inewton", "inewtonWishListLink", "mypassword");

    // This request should fail since the JWT is in the wrong group.
    Response response = processRequest(userServiceURL, "POST", user1.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.UNAUTHORIZED.getStatusCode() + ".",
        Status.UNAUTHORIZED.getStatusCode(),
        response.getStatus());
  }

  /** Tests the update user function. */
  @Test
  public void testUpdateUser() throws Exception {
    // Add user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(
            null,
            "James",
            "Maxwell",
            "jMaxwell",
            "@jMaxwell",
            "jMaxwellWishListLink",
            "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User jMaxwell was NOT found in database.", dbUser != null);
    assertTrue("User jMaxwell does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Update user. Includes a userName update.
    User updatedUser1 =
        new User(
            dbId,
            "James",
            "Maxwell",
            "jamesMaxwell",
            "@jamesMaxwell",
            "jamesMaxwellWishListLink",
            "newPassword");
    String updateUrl = userServiceURL + "/" + dbId;
    response = processRequest(updateUrl, "PUT", updatedUser1.getJson(), authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Validate user update.
    dbUser = (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User jamesMaxwell was NOT found in database.", dbUser != null);
    assertTrue("User jamesMaxwell does not contain expected data.", updatedUser1.isEqual(dbUser));

    // Test 3: Update user not in database.  This should fail.
    updateUrl = userServiceURL + "/" + new ObjectId().toString();
    User updatedUser3 =
        new User(
            null, "Non", "Existent", "NonExistentUserName", "@ne", "neWishListLink", "noPassword");
    response = processRequest(updateUrl, "PUT", updatedUser3.getJson(), authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.BAD_REQUEST.getStatusCode() + ".",
        Status.BAD_REQUEST.getStatusCode(),
        response.getStatus());
  }

  /** Tests the delete user function. */
  @Test
  public void testDeleteUser() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(
            null,
            "Albert",
            "Einstein",
            "aEinstein",
            "@aEinstein",
            "aEinsteinWishListLink",
            "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User aEinstein was NOT found in database.", dbUser != null);
    assertTrue("User aEinstein does not contain expected data.", user.isEqual(dbUser));

    // Delete user.
    String deleteUrl = userServiceURL + "/" + dbId;
    response = processRequest(deleteUrl, "DELETE", null, authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Validate user deletion.
    dbUser = (BasicDBObject) database.getCollection("users").findOne("aEinstein");
    assertTrue("User aEinstein should not have been found in database.", dbUser == null);

    // Test2: Delete the user again.  This should fail.
    response = processRequest(deleteUrl, "DELETE", null, authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.BAD_REQUEST.getStatusCode() + ".",
        Status.BAD_REQUEST.getStatusCode(),
        response.getStatus());
  }

  /** Tests the get user function. */
  @Test
  public void testGetSingleUser() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(
            null,
            "Richard",
            "Feynman",
            "rFeynman",
            "@rFeynman",
            "rFeynmanWishListLink",
            "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Get user data for existing user.
    String getUrl = userServiceURL + "/" + dbId;
    response = processRequest(getUrl, "GET", null, authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Validate that the user was returned and does not contain private data (password).
    JsonObject responseUserObject = toJsonObj(response.readEntity(String.class));
    assertTrue("User rFeynman was NOT found in database.", responseUserObject != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(responseUserObject));
    assertFalse("Response contained a password", responseUserObject.containsKey("password"));

    // Test 2: Get user data for non-existing user.  This should fail.
    getUrl = userServiceURL + "/" + new ObjectId().toString();
    response = processRequest(getUrl, "GET", null, authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.BAD_REQUEST.getStatusCode() + ".",
        Status.BAD_REQUEST.getStatusCode(),
        response.getStatus());
  }

  /** Tests the get user function with the login JWT (should not work). */
  @Test
  public void testGetSingleUserInvalidJwtGroup() throws Exception {
    // Add a user.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User user =
        new User(
            null,
            "Richard",
            "Feynman",
            "rFeynman",
            "@rFeynman",
            "rFeynmanWishListLink",
            "myPassword");
    Response response = processRequest(userServiceURL, "POST", user.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    JsonObject responseJson = toJsonObj(response.readEntity(String.class));
    String dbId = responseJson.getString(KEY_USER_ID);
    user.setId(dbId);

    // Find user in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId));
    assertTrue("User rFeynman was NOT found in database.", dbUser != null);
    assertTrue("User rFeynman does not contain expected data.", user.isEqual(dbUser));

    // Test 1: Get user data for existing user with the JWT we created for login.
    //         This should fail because the JWT is in the wrong group.
    String getUrl = userServiceURL + "/" + dbId;
    response = processRequest(getUrl, "GET", null, loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.UNAUTHORIZED.getStatusCode() + ".",
        Status.UNAUTHORIZED.getStatusCode(),
        response.getStatus());
  }

  /** Tests the get ALL users function. */
  @Test
  public void testGetAllUser() throws Exception {
    // Add 2 users.
    String loginAuthHeader =
        "Bearer "
            + new JWTVerifier()
                .createJWT("unauthenticated", new HashSet<String>(Arrays.asList("login")));
    User gGalilei =
        new User(
            null,
            "Galileo",
            "Galilei",
            "gGalilei",
            "@gGalilei",
            "gGalileiWishListLink",
            "myPassword");
    Response response = processRequest(userServiceURL, "POST", gGalilei.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson1 = toJsonObj(response.readEntity(String.class));
    String dbId1 = responseJson1.getString(KEY_USER_ID);
    gGalilei.setId(dbId1);

    User mCurie =
        new User(null, "Marie", "Curie", "mCurie", "@mCurie", "mCurieWishListLink", "myPassword");
    response = processRequest(userServiceURL, "POST", mCurie.getJson(), loginAuthHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    authHeader = response.getHeaderString("Authorization");
    new JWTVerifier().validateJWT(authHeader);

    JsonObject responseJson2 = toJsonObj(response.readEntity(String.class));
    String dbId2 = responseJson2.getString(KEY_USER_ID);
    mCurie.setId(dbId2);

    // Find users in the database.
    BasicDBObject dbUser =
        (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId1));
    assertTrue("User gGalilei was NOT found in database.", dbUser != null);
    assertTrue("User gGalilei does not contain expected data.", gGalilei.isEqual(dbUser));

    dbUser = (BasicDBObject) database.getCollection("users").findOne(new ObjectId(dbId2));
    assertTrue("User mCurie was NOT found in database.", dbUser != null);
    assertTrue("User mCurie does not contain expected data.", mCurie.isEqual(dbUser));

    // Get users.
    response = processRequest(userServiceURL, "GET", null, authHeader);
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());
    String payload = response.readEntity(String.class);
    assertTrue(
        "Response payload is null. Payload containing list of users is expected.", payload != null);
    JsonObject jObject = toJsonObj(payload);
    JsonArray users = jObject.getJsonArray(DB_USERS_COLLECTION_NAME);

    // Validate users' data.
    for (int i = 0; i < users.size(); i++) {
      JsonObject responseUserObject = users.getJsonObject(i);
      String userName = responseUserObject.getString(User.JSON_KEY_USER_NAME);
      assertFalse(
          "Response contained a password for user " + userName,
          responseUserObject.containsKey("password"));
      if (userName.equals("gGalilei")) {
        assertTrue(
            "User gGalilei does not contain expected data.", gGalilei.isEqual(responseUserObject));
      } else if (userName.equals("mCurie")) {
        assertTrue(
            "User mCurie does not contain expected data.", mCurie.isEqual(responseUserObject));
      } else {
        throw new IllegalStateException("Unknown user was returned: " + userName);
      }
    }
  }

  public Response processRequest(String url, String method, String payload) {
    return processRequest(url, method, payload, null);
  }

  public Response processRequest(String url, String method, String payload, String authHeader) {
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
