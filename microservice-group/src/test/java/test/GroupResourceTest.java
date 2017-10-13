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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GroupResourceTest {

  private static final String groupServiceURL =
      System.getProperty("liberty.test.group.service.url");
  private static MongoClient mongo;
  private static DB db;

  /** Connect to the Mongo database "groups" */
  @BeforeClass
  public static void setup() throws UnknownHostException {
    int mongoPort = Integer.parseInt(System.getProperty("mongo.test.port"));
    String mongoHostname = System.getProperty("mongo.test.hostname");
    mongo = new MongoClient(mongoHostname, mongoPort);
    db = mongo.getDB("gifts-group");
  }

  @AfterClass
  public static void teardown() {
    db.dropDatabase();
    mongo.close();
  }

  /** Make sure the collection is empty before each test */
  @After
  public void testCleanup() {
    db.getCollection("groups").drop();
  }

  /**
   * Call POST to create a new group. Verify that group in database was created with the correct
   * data
   *
   * @throws GeneralSecurityException
   */
  @Test
  public void testCreateGroup() throws IOException, GeneralSecurityException {
    System.out.println("\nStarting testCreateGroup");

    Group group = new Group(null, "testGroup", new String[] {"12345"});

    JsonObject response = makeConnection("POST", groupServiceURL, group.getJson(), 200);

    // Verify that the correct data is in mongo
    String id = response.getString("id");
    group.setId(id);
    BasicDBObject dbGroup =
        (BasicDBObject) db.getCollection(Group.DB_COLLECTION_NAME).findOne(new ObjectId(id));
    assertNotNull("Group testGroup was not found in the database.", dbGroup);
    assertTrue("Group in database does not contain the expected data", group.isEqual(dbGroup));
  }

  /**
   * Add a new group object to the database. Call GET using the id of the new mongo object. Verify
   * group info returned matches the group info in the database
   */
  @Test
  public void testGetGroupInfo() throws Exception {
    System.out.println("\nStarting testGetGroupInfo");

    // Create group in database
    Group group = new Group(null, "testGroup", new String[] {"12345"});
    BasicDBObject dbGroup = group.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbGroup);
    group.setId(dbGroup.getObjectId(Group.DB_ID).toString());

    // Make GET call with group id
    String url = groupServiceURL + "/" + dbGroup.getObjectId(Group.DB_ID);
    JsonObject response = makeConnection("GET", url, null, 200);

    Group returnedGroup = new Group(response.toString());

    // Verify the returned group info is correct
    assertTrue(
        "Group info returned does not match the group info in the database ",
        group.isEqual(returnedGroup));
  }

  /**
   * Add a new group objects to the database. Call GET using the id of the user. Verify group info
   * returned matches the group info in the database.
   */
  @Test
  public void testGetGroupsForUser() throws Exception {
    System.out.println("\nStarting testGetGroupsForUser");

    // create some users
    BasicDBObject user = new BasicDBObject();
    user.append("userName", "JorgeWashington");
    db.getCollection("testUsers").insert(user);
    String jorgeWashingtonDbId = user.getString("_id");

    user = new BasicDBObject();
    user.append("userName", "JamesMonroe");
    db.getCollection("users").insert(user);
    String jamesMonroeDbId = user.getString("_id");

    user = new BasicDBObject();
    user.append("userName", "ThomasJefferson");
    db.getCollection("users").insert(user);
    String thomasJeffersonDbId = user.getString("_id");

    // Create groups in database
    Group group1 =
        new Group(null, "birthdayGroup", new String[] {jorgeWashingtonDbId, thomasJeffersonDbId});
    BasicDBObject dbgroup1 = group1.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup1);
    group1.setId(dbgroup1.getObjectId(Group.DB_ID).toString());

    Group group2 =
        new Group(null, "friendGroup", new String[] {jorgeWashingtonDbId, jamesMonroeDbId});
    BasicDBObject dbgroup2 = group2.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup2);
    group2.setId(dbgroup2.getObjectId(Group.DB_ID).toString());

    Group group3 = new Group(null, "gymGroup", new String[] {thomasJeffersonDbId, jamesMonroeDbId});
    BasicDBObject dbgroup3 = group3.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup3);
    group3.setId(dbgroup3.getObjectId(Group.DB_ID).toString());

    // Make GET call get all groups associated to the specified user ID.
    String url = groupServiceURL + "?userId=" + jorgeWashingtonDbId;
    JsonObject response = makeConnection("GET", url, null, 200);

    // Verify the returned group info is correct
    assertTrue("Invalid response.", response != null);
    JsonArray groupsInResponse = (JsonArray) response.get(Group.JSON_KEY_GROUPS_FOR_USER_LIST);
    assertTrue("No groups found in response.", groupsInResponse != null);

    boolean birthdayGroupFound = false;
    boolean friendGroupFound = false;
    for (int i = 0; i < groupsInResponse.size(); i++) {
      Group returnedGroup = new Group(groupsInResponse.getString(i));
      if (group1.isEqual(returnedGroup)) {
        birthdayGroupFound = true;
        continue;
      }
      if (group2.isEqual(returnedGroup)) {
        friendGroupFound = true;
        continue;
      }
    }

    assertTrue(
        "The number of expected groups or groups data are not expected. Expected groups for user is 2. Found: birthdayGroup: "
            + birthdayGroupFound
            + ", friendGroup: "
            + friendGroupFound,
        (friendGroupFound && birthdayGroupFound));
  }

  /** Add a new group objects to the database. Call GET. Verify that all groups are returned. */
  @Test
  public void testGetAllGroups() throws Exception {
    System.out.println("\nStarting testGetAllGroups");

    // create some users
    BasicDBObject user = new BasicDBObject();
    user.append("userName", "JorgeWashington");
    db.getCollection("testUsers").insert(user);
    String jorgeWashingtonDbId = user.getString("_id");

    user = new BasicDBObject();
    user.append("userName", "JamesMonroe");
    db.getCollection("users").insert(user);
    String jamesMonroeDbId = user.getString("_id");

    user = new BasicDBObject();
    user.append("userName", "ThomasJefferson");
    db.getCollection("users").insert(user);
    String thomasJeffersonDbId = user.getString("_id");

    // Create groups in database
    // Create groups in database
    Group group1 =
        new Group(null, "birthdayGroup", new String[] {jorgeWashingtonDbId, thomasJeffersonDbId});
    BasicDBObject dbgroup1 = group1.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup1);
    group1.setId(dbgroup1.getObjectId(Group.DB_ID).toString());

    Group group2 =
        new Group(null, "friendGroup", new String[] {jorgeWashingtonDbId, jamesMonroeDbId});
    BasicDBObject dbgroup2 = group2.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup2);
    group2.setId(dbgroup2.getObjectId(Group.DB_ID).toString());

    Group group3 = new Group(null, "gymGroup", new String[] {thomasJeffersonDbId, jamesMonroeDbId});
    BasicDBObject dbgroup3 = group3.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbgroup3);
    group3.setId(dbgroup3.getObjectId(Group.DB_ID).toString());

    // Make GET call get all groups.
    JsonObject response = makeConnection("GET", groupServiceURL, null, 200);

    // Verify the returned group info is correct
    assertTrue("Invalid response.", response != null);
    JsonArray groupsInResponse = (JsonArray) response.get(Group.JSON_KEY_GROUPS_FOR_USER_LIST);
    assertTrue("No groups found in response.", groupsInResponse != null);

    boolean birthdayGroupFound = false;
    boolean friendGroupFound = false;
    boolean gymGroupFound = false;
    for (int i = 0; i < groupsInResponse.size(); i++) {
      Group returnedGroup = new Group(groupsInResponse.getString(i));
      if (group1.isEqual(returnedGroup)) {
        birthdayGroupFound = true;
        continue;
      }
      if (group2.isEqual(returnedGroup)) {
        friendGroupFound = true;
        continue;
      }
      if (group3.isEqual(returnedGroup)) {
        gymGroupFound = true;
        continue;
      }
    }

    assertTrue(
        "The number of expected groups or groups data are not expected. Expected groups for user is 2. Found: birthdayGroup: "
            + birthdayGroupFound
            + ", friendGroup: "
            + friendGroupFound
            + ", gymGroup: "
            + gymGroupFound,
        (friendGroupFound && birthdayGroupFound && gymGroupFound));
  }

  /** Try to get all groups with no JWT in the authorization header. */
  @Test
  public void testGetAllGroupsNoJWT() throws Exception {
    System.out.println("\nStarting testGetAllGroupsNoJWT");

    // Make GET call get all groups.  Note that early versions of the
    // mpJwt-1.0 feature would return an HTTP 401 when accessing an
    // unprotected resource with no JWT, and later versions would return
    // a 500 due to an NPE.  We'll accept either here.
    makeConnection("GET", groupServiceURL, null, 200, "users");
    makeConnection("GET", groupServiceURL, null, new Integer[] {401, 500}, null);
  }

  /** Try to get all groups as an invalid user (invalid group). */
  @Test
  public void testGetAllGroupsInvalidJWTGroup() throws Exception {
    System.out.println("\nStarting testGetAllGroupsInvalidJWTGroup");

    // Make GET call get all groups.
    makeConnection("GET", groupServiceURL, null, 200, "users");
    makeConnection("GET", groupServiceURL, null, 200, "orchestrator");
    makeConnection("GET", groupServiceURL, null, 401, "unauthGroup");
  }

  /**
   * Add a new group object to the database. Call DELETE using the id of the new mongo object.
   * Verify that the group no longer exists in the database
   *
   * @throws GeneralSecurityException
   */
  @Test
  public void testDeleteGroup() throws IOException, GeneralSecurityException {
    System.out.println("\nStarting testDeleteGroup");

    // Create group in database
    Group group = new Group(null, "testGroup", new String[] {"12345"});
    BasicDBObject dbGroup = group.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbGroup);
    group.setId(dbGroup.getObjectId(Group.DB_ID).toString());

    ObjectId groupId = dbGroup.getObjectId(Group.DB_ID);
    // Make DELETE call with group id
    String url = groupServiceURL + "/" + groupId;
    makeConnection("DELETE", url, null, 200);

    // Verify that the group no longer exists in mongo
    BasicDBObject groupAfterDelete = (BasicDBObject) db.getCollection("groups").findOne(groupId);
    assertNull("The group still exists after DELETE was called", groupAfterDelete);
  }

  /**
   * Call DELETE using a no existent id. Verify that the right response code is returned
   *
   * @throws GeneralSecurityException
   */
  @Test
  public void testDeleteNonExistentGroup() throws IOException, GeneralSecurityException {
    System.out.println("\nStarting testDeleteNonExistentGroup");

    String groupId = "123456789";

    // Make DELETE call with non-existent group id
    String url = groupServiceURL + "/" + groupId;
    makeConnection("DELETE", url, null, 400);
  }

  /**
   * Call GET using a non existent id. Verify that the right response code is returned
   *
   * @throws GeneralSecurityException
   */
  @Test
  public void testGetNonExistentGroup() throws IOException, GeneralSecurityException {
    System.out.println("\nStarting testGetNonExistentGroup");

    String groupId = "123456789";

    // Make DELETE call with non-existent group id
    String url = groupServiceURL + "/" + groupId;
    makeConnection("GET", url, null, 400);
  }

  /**
   * Add a new group object to the database. Call PUT using the id of the new mongo object to update
   * the group information (name, members list, occasions list). Verify that the group information
   * has been updated
   *
   * @throws GeneralSecurityException
   */
  @Test
  public void testUpdateGroup() throws IOException, GeneralSecurityException {
    System.out.println("\nStarting testUpdateGroup");

    // Create group in database
    Group group = new Group(null, "testGroup", new String[] {"12345", "23456"});
    BasicDBObject dbGroup = group.getDBObject(false);
    db.getCollection(Group.DB_COLLECTION_NAME).insert(dbGroup);
    group.setId(dbGroup.getObjectId(Group.DB_ID).toString());

    // Create updated group
    ObjectId groupId = dbGroup.getObjectId(Group.DB_ID);
    Group newGroup = new Group(groupId.toString(), "newTestGroup", new String[] {"12345"});
    String url = groupServiceURL + "/" + groupId;
    makeConnection("PUT", url, newGroup.getJson(), 200);

    // Verify that the new group information is in mongo
    BasicDBObject newDBGroup = (BasicDBObject) db.getCollection("groups").findOne(groupId);
    assertNotNull("Group testGroup was not found in the database.", newDBGroup);
    assertTrue(
        "Group in database does not contain the expected data", newGroup.isEqual(newDBGroup));
  }

  /** Make an HTTP connection to the specified URL and pass in the specified payload */
  private JsonObject makeConnection(
      String method, String urlString, String payload, int expectedResponseCode)
      throws IOException, GeneralSecurityException {
    return makeConnection(method, urlString, payload, expectedResponseCode, "users");
  }

  private JsonObject makeConnection(
      String method, String urlString, String payload, int expectedResponseCode, String group)
      throws IOException, GeneralSecurityException {
    return makeConnection(method, urlString, payload, new Integer[] {expectedResponseCode}, group);
  }

  private JsonObject makeConnection(
      String method,
      String urlString,
      String payload,
      Integer[] acceptableResponseCodes,
      String group)
      throws IOException, GeneralSecurityException {

    // Setup connection
    System.out.println("Creating connection - Method: " + method + ", URL: " + urlString);

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(urlString);
    Response response;

    String jwt = null;
    if (group != null) {
      Set<String> groups = new HashSet<String>(Arrays.asList(group));
      jwt = new JWTVerifier().createJWT("fred", groups);
    }

    if (payload != null) {
      // Send JSON payload
      System.out.println("Request Payload: " + payload);
      Invocation.Builder invoBuild = target.request(MediaType.APPLICATION_JSON_TYPE);
      if (jwt != null) {
        invoBuild.header("Authorization", "Bearer " + jwt);
      }
      Entity<String> data = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
      response = invoBuild.build(method, data).invoke();
    } else {
      // No JSON payload to send
      Invocation.Builder invoBuild = target.request();
      if (jwt != null) {
        invoBuild.header("Authorization", "Bearer " + jwt);
      }
      response = invoBuild.build(method).invoke();
    }

    int actualResponseCode = response.getStatus();
    System.out.println("Response: " + actualResponseCode);
    // Verify that the response code is as expected
    assertTrue(
        "HTTP response code (" + actualResponseCode + ") is not as expected",
        Arrays.asList(acceptableResponseCodes).contains(Integer.valueOf(actualResponseCode)));

    String responseString = response.readEntity(String.class);
    response.close();

    if (responseString != null && !responseString.trim().equals("")) {
      System.out.println("Response Payload: " + responseString);
    }

    return Group.stringToJsonObj(responseString);
  }
}
