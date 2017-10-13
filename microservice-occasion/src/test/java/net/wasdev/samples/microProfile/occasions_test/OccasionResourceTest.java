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
package net.wasdev.samples.microProfile.occasions_test;

import static org.junit.Assert.assertTrue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.wasdev.samples.microProfile.occasions.Occasion;
import org.bson.types.ObjectId;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import test.JWTVerifier;

/** The main junit test class for the Occasion Service */
public class OccasionResourceTest {
  private static MongoClient mongoClient;
  private static DB db;
  private static DBCollection collection;

  private static final String clazz = OccasionResourceTest.class.getName();
  private static final Logger logger = Logger.getLogger(clazz);

  private static final String occasionServiceURL =
      System.getProperty("liberty.test.occasion.service.url");
  private static final int mongoPort = Integer.parseInt(System.getProperty("mongo.test.port"));
  private static final String mongoHostname = System.getProperty("mongo.test.hostname");

  @Rule public TestName name = new TestName();

  /** Connect to the Occasions database and set the collection */
  @BeforeClass
  public static void beforeClass() throws UnknownHostException {
    String method = "beforeClass";
    logger.entering(clazz, method);
    mongoClient = new MongoClient(mongoHostname, mongoPort);
    db = mongoClient.getDB("gifts-occasion");
    collection = db.getCollection("occasions");
    logger.exiting(clazz, method);
  }

  /** Cleanup the database */
  @AfterClass
  public static void cleanup() {
    String method = "cleanup";
    logger.entering(clazz, method);
    db.dropDatabase();
    mongoClient.close();
    logger.exiting(clazz, method);
  }

  /** Let each test start with a blank db. */
  @Before
  public void beforeTest() {
    logger.entering(clazz, name.getMethodName());
    logger.fine(collection.remove(new BasicDBObject()).toString());
    logger.exiting(clazz, name.getMethodName());
  }

  /** Test the get GET call failure with a bad groupId */
  @Test
  public void testGETBadGroup() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload for occasion 1
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0001", 20));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "1111",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0001",
            /* recipient ID  */ "0997",
            contributions);

    collection.insert(occasion.toDbo());

    // build the json payload for occasion 2
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0002", 50));
    contributions.add(new Occasion.Contribution("0003", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-09-30",
            /* group ID      */ "2222",
            /* interval      */ "annual",
            /* occasion name */ "Jill Doe's Birthday",
            /* organizer ID  */ "0002",
            /* recipient ID  */ "0998",
            contributions);

    collection.insert(occasion.toDbo());

    // get the ID
    occasion = new Occasion(collection.findOne(occasion.toDbo()));

    // build the json payload for occasion 3
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("9999", 20));
    contributions.add(new Occasion.Contribution("8888", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2018-01-31",
            /* group ID      */ "3333",
            /* interval      */ "annual",
            /* occasion name */ "Jason Doe's Birthday",
            /* organizer ID  */ "8888",
            /* recipient ID  */ "9999",
            contributions);

    collection.insert(occasion.toDbo());

    // verify that there are 3 occsions in the db
    long expectedCount = 3;
    long count = collection.count();
    assertTrue(
        "The check that all occasions are in the db failed. Expected: "
            + expectedCount
            + "\n\nResult:\n\n"
            + count,
        count == expectedCount);

    // confirm that we get a server 400
    testEndpoint("/?groupId=xxxx", "GET", "", 200);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Test the get GET call by creating some occasions and then listing one of them. */
  @Test
  public void testGET() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload for occasion 1
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0001", 20));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "1111",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0001",
            /* recipient ID  */ "0997",
            contributions);

    collection.insert(occasion.toDbo());

    // build the json payload for occasion 2
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0002", 50));
    contributions.add(new Occasion.Contribution("0003", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-09-30",
            /* group ID      */ "2222",
            /* interval      */ "annual",
            /* occasion name */ "Jill Doe's Birthday",
            /* organizer ID  */ "0002",
            /* recipient ID  */ "0998",
            contributions);

    collection.insert(occasion.toDbo());

    // get the ID
    occasion = new Occasion(collection.findOne(occasion.toDbo()));

    String expectedFinal = occasion.toString();
    String occasionToGet = occasion.getId().toString();

    // build the json payload for occasion 3
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("9999", 20));
    contributions.add(new Occasion.Contribution("8888", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2018-01-31",
            /* group ID      */ "3333",
            /* interval      */ "annual",
            /* occasion name */ "Jason Doe's Birthday",
            /* organizer ID  */ "8888",
            /* recipient ID  */ "9999",
            contributions);

    collection.insert(occasion.toDbo());

    // verify that there are 3 occsions in the db
    long expectedCount = 3;
    long count = collection.count();
    assertTrue(
        "The check that all occasions are in the db failed. Expected: "
            + expectedCount
            + "\n\nResult:\n\n"
            + count,
        count == expectedCount);

    // confirm that we can retrieve just one
    testEndpoint("/" + occasionToGet, "GET", expectedFinal, 200);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Send a basic POST request to create and occasion with a json payload */
  @Test
  public void testPOST() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload for occasion
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0001", 20));
    contributions.add(new Occasion.Contribution("0002", 50));
    contributions.add(new Occasion.Contribution("0003", 50));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "0001",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0001",
            /* recipient ID  */ "2999",
            contributions);

    String responseString = testEndpointJson("/", "POST", occasion.toString(), "", 200);

    String id = (String) ((DBObject) JSON.parse(responseString)).get(Occasion.OCCASION_ID_KEY);
    logger.fine("id = " + id);
    ObjectId oid = new ObjectId(id);
    BasicDBObject queryObj = new BasicDBObject(Occasion.OCCASION_ID_KEY, oid);

    DBObject dbo = collection.findOne(queryObj);
    logger.fine("After findOne with dbo: " + dbo);
    String expected = new Occasion(dbo).getId().toString();

    assertTrue(
        "Mismatch in "
            + name.getMethodName()
            + ". Expected:\n\n"
            + expected
            + "\n\nRecieved:\n\n"
            + responseString,
        responseString.contains(expected));

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Similar to testPOST, but this test sends a payload with an id field */
  @Test(expected = IllegalArgumentException.class)
  public void testPOSTWithId() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0004", 21));
    contributions.add(new Occasion.Contribution("0005", 51));
    contributions.add(new Occasion.Contribution("0006", 52));
    Occasion occasion =
        new Occasion(
            /* ID            */ new ObjectId("1000"),
            /* date          */ "2117-07-31",
            /* group ID      */ "0002",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0004",
            /* recipient ID  */ "0006",
            contributions);

    // expecting 400 because POST is for creating a new occasion and should not include an ID
    testEndpointJson("/", "POST", occasion.toString(), "IllegalArgumentException", 400);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Call POST with garbage, expecting a response code 400 */
  @Test
  public void testPOSTFailure() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    testEndpointJson("/", "POST", null, "", 400);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /**
   * Test basic PUT call to update an occasion with a json payload.
   *
   * <p>First it creates an occasion then it updates it
   */
  @Test
  public void testPUT() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0007", 22));
    contributions.add(new Occasion.Contribution("0008", 53));
    contributions.add(new Occasion.Contribution("0009", 54));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "0003",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0007",
            /* recipient ID  */ "0097",
            contributions);

    // create the initial occasion.  Need to use POST and not put it in the
    // database directly, or else the orchestrator won't know about it and will
    // throw an NPE.
    String responseString = testEndpointJson("/", "POST", occasion.toString(), "", 200);
    String id = (String) ((DBObject) JSON.parse(responseString)).get(Occasion.OCCASION_ID_KEY);
    logger.fine("id = " + id);
    DBObject dbo =
        collection.findOne(new BasicDBObject(Occasion.OCCASION_ID_KEY, new ObjectId(id)));
    occasion = new Occasion(dbo);

    String jsonUpdate =
        Json.createObjectBuilder()
            .add(Occasion.OCCASION_NAME_KEY, "Johnny D's Birthday")
            .build()
            .toString();

    // update the expected string
    occasion.setName("Johnny D's Birthday");

    // update the occasion
    testEndpointJson("/" + occasion.getId(), "PUT", jsonUpdate, "", 200);
    DBObject resultDbo =
        collection.findOne(new BasicDBObject(Occasion.OCCASION_ID_KEY, occasion.getId()));
    Occasion resultOccasion = new Occasion(resultDbo);

    assertTrue(
        name.getMethodName()
            + ": PUT result does not match. Expected:\n\n"
            + occasion.toString()
            + "\n\nResult:\n\n"
            + resultOccasion
            + "\n\n",
        occasion.equals(resultOccasion));

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Test PUT failure. Expects to get a response code 400. */
  @Test
  public void testPUTFailure() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    testEndpointJson("/xxxx", "PUT", null, "", 400);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Test the deletion of an existing occasion. Add it, then delete it. */
  @Test
  public void testDELETE() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    // build the json payload
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0010", 23));
    contributions.add(new Occasion.Contribution("0011", 55));
    contributions.add(new Occasion.Contribution("0012", 56));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "0004",
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0010",
            /* recipient ID  */ "9999",
            contributions);

    // create the initial occasion.  Need to use POST and not put it in the
    // database directly, or else the orchestrator won't know about it and will
    // throw an NPE.
    String responseString = testEndpointJson("/", "POST", occasion.toString(), "", 200);
    String id = (String) ((DBObject) JSON.parse(responseString)).get(Occasion.OCCASION_ID_KEY);
    logger.fine("id = " + id);
    DBObject dbo =
        collection.findOne(new BasicDBObject(Occasion.OCCASION_ID_KEY, new ObjectId(id)));
    occasion = new Occasion(dbo);

    testEndpoint("/" + id, "DELETE", "", 200);

    dbo =
        (BasicDBObject)
            collection.findOne(new BasicDBObject(Occasion.OCCASION_ID_KEY, occasion.getId()));

    assertTrue("DBObject should be null. Instead it was:\n\n" + dbo, null == dbo);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Test the failure of the DELETE call. Expecting a response code 400. */
  @Test
  public void testDELETEFailure() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    testEndpoint("/xxxx", "DELETE", "", 400);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /** Test the ability to retrieve a list of occasions for a given group */
  @Test
  public void testOccasionsForGroup() {
    logger.entering(
        clazz, name.getMethodName(), "\n\n+ + + + + Entering " + name.getMethodName() + "\n\n");

    String groupId = "3333";

    /*
     * build the json payload for the occasions
     */
    JsonBuilderFactory factory = Json.createBuilderFactory(null);

    // collect the expected output in this
    JsonArrayBuilder resultsBuilder = factory.createArrayBuilder();

    // build the json payload for occasion 1
    List<Occasion.Contribution> contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0001", 20));
    Occasion occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-07-31",
            /* group ID      */ "1111", // different ID than the others in this test so it's omitted
            // from the results
            /* interval      */ "annual",
            /* occasion name */ "John Doe's Birthday",
            /* organizer ID  */ "0001",
            /* recipient ID  */ "9998",
            contributions);

    // insert the occasion into the db
    collection.insert(occasion.toDbo());
    // occasion1 belongs to a different group. omitted from expected results

    // build the json payload for occasion 2
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("0002", 50));
    contributions.add(new Occasion.Contribution("0003", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2117-09-30",
            /* group ID      */ groupId,
            /* interval      */ "annual",
            /* occasion name */ "Jill Doe's Birthday",
            /* organizer ID  */ "0002",
            /* recipient ID  */ "9997",
            contributions);

    // insert the occasion into the db and add the result to the expected result array
    collection.insert(occasion.toDbo());
    occasion = new Occasion(collection.findOne(occasion.toDbo()));
    resultsBuilder.add(occasion.toJson());

    // build the json payload for occasion 3
    contributions = new ArrayList<>();
    contributions.add(new Occasion.Contribution("9999", 20));
    contributions.add(new Occasion.Contribution("8888", 50));
    occasion =
        new Occasion(
            /* ID            */ null,
            /* date          */ "2018-01-31",
            /* group ID      */ groupId,
            /* interval      */ "annual",
            /* occasion name */ "Jason Doe's Birthday",
            /* organizer ID  */ "9999",
            /* recipient ID  */ "9996",
            contributions);

    // insert the occasion into the db and add the result to the expected result array
    collection.insert(occasion.toDbo());
    occasion = new Occasion(collection.findOne(occasion.toDbo()));
    resultsBuilder.add(occasion.toJson());

    testEndpoint("/?groupId=" + groupId, "GET", resultsBuilder.build().toString(), 200);

    logger.exiting(
        clazz, name.getMethodName(), "\n\n- - - - - Exiting " + name.getMethodName() + "\n\n");
  }

  /**
   * Helper method for testing request success/failure
   *
   * @param endpoint the resource to call
   * @param requestType the http method to invoke
   * @param expectedOutput the response string we expect from the server to pass the test
   * @param expectedResponseCode the response code we expect from the server to pass the test
   */
  public void testEndpoint(
      String endpoint, String requestType, String expectedOutput, int expectedResponseCode) {
    String method = "testEndpoint";
    logger.entering(clazz, method);
    logger.fine("  endpoint: " + endpoint);
    logger.fine("  requestType: " + requestType);
    logger.fine("  expectedOutput: " + expectedOutput);
    logger.fine("  expectedResponseCode: " + expectedResponseCode);

    // build the url
    String url = occasionServiceURL + endpoint;
    logger.fine("url: " + url);

    // make the call and check the response
    Response response = sendRequest(url, requestType);
    int responseCode = response.getStatus();
    logger.fine("responseCode: " + responseCode);
    assertTrue(
        "Incorrect response code: "
            + responseCode
            + "\nexpected response code: "
            + expectedResponseCode,
        responseCode == expectedResponseCode);

    String responseString = response.readEntity(String.class);
    logger.fine("testEndpoint responseString: " + responseString);
    response.close();
    assertTrue(
        "Incorrect response, response is: \n\n"
            + responseString
            + "\n\n expected: \n\n"
            + expectedOutput
            + "\n\n",
        responseString.contains(expectedOutput));
    logger.exiting(clazz, method, "\n\n- - - - - Exiting " + method + "\n\n");
  }

  /**
   * Helper method for testing requests that are supposed to succeed
   *
   * @param endpoint the resource to call
   * @param requestType the http method to invoke
   * @param json the json payload to send
   * @param expectedOutput the response string we expect from the server to pass the test
   * @param expectedResponseCode the response code we expect from the server to pass the test
   * @return the response
   */
  public String testEndpointJson(
      String endpoint,
      String requestType,
      String json,
      String expectedOutput,
      int expectedResponseCode) {
    String method = "testEndpointJson";
    logger.entering(clazz, method);
    logger.fine("  endpoint: " + endpoint);
    logger.fine("  requestType: " + requestType);
    logger.fine("  json: " + json);
    logger.fine("  expectedOutput: " + expectedOutput);
    logger.fine("  expectedResponseCode: " + expectedResponseCode);

    // build url
    String url = occasionServiceURL + endpoint;
    logger.fine("url: " + url);

    String responseString = "";
    try {
      // make the call and check the response
      Response response = sendRequestJson(url, requestType, json);
      int responseCode = response.getStatus();
      logger.fine("responseCode: " + responseCode);
      assertTrue(
          "Incorrect response code: "
              + responseCode
              + "\nexpected response code: "
              + expectedResponseCode,
          responseCode == expectedResponseCode);

      responseString = response.readEntity(String.class);
      response.close();
      logger.fine("testEndpointJson responseString: " + responseString);
      assertTrue(
          "Incorrect response, response is: \n\n"
              + responseString
              + "\n\n expected: \n\n"
              + expectedOutput
              + "\n\n",
          responseString.contains(expectedOutput));
    } catch (Throwable t) {
      // some tests deliberately pass garbage
      assertTrue(
          "In " + method + " with unexpected exception: " + t.getMessage(),
          t instanceof IllegalArgumentException
              && expectedOutput.equals("IllegalArgumentException"));
    }
    logger.exiting(
        clazz, method, "\n\n- - - - - Exiting " + method + " with: " + responseString + "\n\n");
    return responseString;
  }

  /**
   * Helper method to send a simple http request without a payload
   *
   * @param url the url to call
   * @param requestType the http method to invoke
   * @return the response from the server
   */
  public Response sendRequest(String url, String requestType) {
    String method = "sendRequest";
    logger.entering(clazz, method);
    logger.fine("  url: " + url);
    logger.fine("  requestType: " + requestType);

    String jwt = null;
    try {
      jwt = new JWTVerifier().createJWT("fred");
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(url);
    Invocation.Builder invoBuild = target.request();

    invoBuild.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    Response response = invoBuild.build(requestType).invoke();

    logger.exiting(
        clazz, method, "\n\n- - - - - Exiting " + method + " with: " + response + "\n\n");
    return response;
  }

  /**
   * Helper method for sending requests with a json payload.
   *
   * @param url the url to call
   * @param requestType the http method to invoke
   * @param json the json payload to send
   * @return the response from the server
   */
  public Response sendRequestJson(String url, String requestType, String json) {
    String method = "sendRequestJson";
    logger.entering(clazz, method);
    logger.fine("  url: " + url);
    logger.fine("  requestType: " + requestType);
    logger.fine("  json: " + json);

    String jwt = null;
    try {
      jwt = new JWTVerifier().createJWT("fred");
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(url);
    Invocation.Builder invoBuild = target.request(MediaType.APPLICATION_JSON_TYPE);

    // Allowing for null payload to get a 400 response code
    Entity<String> data;
    if (null == json || json.isEmpty()) {
      data = Entity.entity("", MediaType.APPLICATION_JSON_TYPE);
    } else {
      data = Entity.entity(json, MediaType.APPLICATION_JSON_TYPE);
    }

    invoBuild.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    Response response = invoBuild.build(requestType, data).invoke();

    logger.exiting(
        clazz,
        method,
        "\n\n- - - - - Exiting " + method + " with: " + response.readEntity(String.class) + "\n\n");
    return response;
  }
}
