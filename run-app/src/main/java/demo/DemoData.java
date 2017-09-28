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
package demo;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Populates the pertinent databases. */
public class DemoData {

  private static final String KEY_ID = "id";
  private static final String KEY_OCCASION_DATE = "year-month-day";

  private static final String BOOT_FILE_USERS = "users.json";
  private static final String BOOT_FILE_GROUPS = "groups.json";
  private static final String BOOT_FILE_OCCASIONS = "occasions.json";

  private static final int OCCASION_DATE_FUDGE_LOWER_BOUND = 90;
  private static final int OCCASION_DATE_FUDGE_RANDOM_UPPER_BOUND = 200;

  private static String userServiceURL;
  private static String groupServiceURL;
  private static String occasionServiceURL;
  private static String authServiceURL;

  // Map temp ids in json files to the official ids returned when the objects
  // are added to their respective services
  private static HashMap<String, String> userIds = new HashMap<String, String>();
  private static HashMap<String, String> groupIds = new HashMap<String, String>();

  // JWT returned from user service to use for creating groups and occasions
  private static String jwt = null;

  public static void main(String[] args) {

    if (args.length != 8) {
      System.err.println(
          "Usage: This jar expects eight arguments specifying host and ports for four services in the following order: \n\n"
              + "user server hostname\n"
              + "user server https port\n"
              + "group server hostname\n"
              + "group server https port\n"
              + "occasion server hostname\n"
              + "occasion server https port\n"
              + "auth server hostname\n"
              + "auth server https port");

      System.exit(1);
    }

    String userHost = args[0];
    String userPort = args[1];
    String groupHost = args[2];
    String groupPort = args[3];
    String occasionHost = args[4];
    String occasionPort = args[5];
    String authHost = args[6];
    String authPort = args[7];

    userServiceURL = "https://" + userHost + ":" + userPort + "/users";
    groupServiceURL = "https://" + groupHost + ":" + groupPort + "/groups";
    occasionServiceURL = "https://" + occasionHost + ":" + occasionPort + "/occasions";
    authServiceURL = "https://" + authHost + ":" + authPort + "/auth";

    try {
      parseUsers();
      parseGroups();
      parseOccasions();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  /** Populates the user microservice database with pre-defined users. */
  private static void parseUsers() throws IOException {
    final URL users = Thread.currentThread().getContextClassLoader().getResource(BOOT_FILE_USERS);
    assert users != null : "Failed to load '" + BOOT_FILE_USERS + "'";

    final JsonReaderFactory factory = Json.createReaderFactory(null);
    JsonReader reader = factory.createReader(users.openStream());

    final JsonArray jsonUsers = reader.readArray();

    // Get JWT to use when creating users
    String authJWT = null;
    try {
      Response authResponse = makeConnection("GET", authServiceURL, null, null);
      authJWT = authResponse.getHeaderString("Authorization");
    } catch (GeneralSecurityException e) {
      System.err.println("Could not connect to the auth service");
    }

    // Send request to user service to add new users
    for (JsonValue userJsonValue : jsonUsers) {
      try {
        String userPayload = userJsonValue.toString();

        Response response = makeConnection("POST", userServiceURL, userPayload, authJWT);
        if (jwt == null) {
          // Get the jwt returned from the user service. We only need
          // this from the first call to create a user.
          jwt = response.getHeaderString("Authorization");
        }
        // Check http response code
        int rc = response.getStatus();
        System.out.println("RC: " + rc);

        if (rc != 200) {
          System.err.println("Add user failed");
          System.exit(1);
        }

        // Add official id to user map
        System.out.println(response.readEntity(String.class));
        JsonObject responseJson = toJsonObj(response.readEntity(String.class));
        String officialId = responseJson.getString(KEY_ID);
        String tempId = toJsonObj(userPayload).getString(KEY_ID);

        userIds.put(tempId, officialId);

      } catch (IOException | GeneralSecurityException e) {
        System.err.println("Could not connect to the user service");
      }
    }
  }

  /** Populates the group microservice database with pre-defined groups. */
  private static void parseGroups() throws IOException {
    final URL groups = Thread.currentThread().getContextClassLoader().getResource(BOOT_FILE_GROUPS);
    assert groups != null : "Failed to load '" + BOOT_FILE_GROUPS + "'";

    final JsonReaderFactory factory = Json.createReaderFactory(null);
    JsonReader reader = factory.createReader(groups.openStream());

    final JsonArray jsonGroups = reader.readArray();

    // Send request to user service to add new groups
    for (JsonValue groupJsonValue : jsonGroups) {
      try {
        String groupPayload = groupJsonValue.toString();

        // Replace userIds with official userIds
        for (String userId : userIds.keySet()) {
          groupPayload = groupPayload.replaceAll(userId, userIds.get(userId));
        }

        Response response = makeConnection("POST", groupServiceURL, groupPayload, jwt);

        // Check rc
        int rc = response.getStatus();
        System.out.println("RC: " + rc);

        if (rc != 200) {
          System.err.println("Add group failed");
          System.exit(1);
        }

        // Add official id to group map
        JsonObject groupJsonObject = toJsonObj(groupPayload);
        JsonObject responseJson = toJsonObj(response.readEntity(String.class));
        String officialId = responseJson.getString(KEY_ID);
        String tempId = groupJsonObject.getString(KEY_ID);

        groupIds.put(tempId, officialId);

      } catch (IOException | GeneralSecurityException e) {
        System.err.println("Could not connect to the group service");
      }
    }
  }

  /** Populates the Occasion microservice database with pre-defined occasions. */
  private static void parseOccasions() throws IOException {
    final URL occasions =
        Thread.currentThread().getContextClassLoader().getResource(BOOT_FILE_OCCASIONS);
    assert occasions != null : "Failed to load '" + BOOT_FILE_OCCASIONS + "'";

    final JsonReaderFactory factory = Json.createReaderFactory(null);
    JsonReader reader = factory.createReader(occasions.openStream());

    final JsonArray jsonOccasions = reader.readArray();

    // Send a request to the occasion service to add new occasions.
    Random random = new Random();
    for (JsonValue occasionJsonValue : jsonOccasions) {
      try {

        String occasionPayload = occasionJsonValue.toString();

        // Replace userIds with official userIds
        for (String userId : userIds.keySet()) {
          occasionPayload = occasionPayload.replaceAll(userId, userIds.get(userId));
        }

        // Replace groupIds with official groupIds
        for (String groupId : groupIds.keySet()) {
          occasionPayload = occasionPayload.replaceAll(groupId, groupIds.get(groupId));
        }

        // Replace the date entry with a date between 90 and 200 days
        // from the current date.
        int fudge =
            OCCASION_DATE_FUDGE_LOWER_BOUND
                + random.nextInt(OCCASION_DATE_FUDGE_RANDOM_UPPER_BOUND);
        occasionPayload = calculateOccasionDate(occasionPayload, fudge);

        Response response = makeConnection("POST", occasionServiceURL, occasionPayload, jwt);

        // Check rc
        int rc = response.getStatus();
        System.out.println("RC: " + rc);

        if (rc != 200) {
          System.err.println("Add occasion failed");
          System.exit(1);
        }

      } catch (IOException | GeneralSecurityException e) {
        System.err.println("Could not connect to the occasion service");
      }
    }
  }

  /** Make an HTTP connection to the specified URL and pass in the specified payload. */
  private static Response makeConnection(
      String method, String urlString, String payload, String jwt)
      throws IOException, GeneralSecurityException {

    // Setup connection
    System.out.println("Creating connection - Method: " + method + ", URL: " + urlString);

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(urlString);

    Invocation.Builder invoBuild = target.request(MediaType.APPLICATION_JSON_TYPE);

    if (jwt != null) {
      invoBuild.header("Authorization", jwt);
    }
    if (payload != null) {
      System.out.println("Request Payload: " + payload);
      Entity<String> data = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
      return invoBuild.build(method, data).invoke();
    } else {
      return invoBuild.build(method).invoke();
    }
  }

  /**
   * Converts a JSON string to a JSONObject instance.
   *
   * @param json The JSON string to convert.
   * @return The JSONObject instance representing the input.
   */
  public static JsonObject toJsonObj(String json) {
    try (JsonReader jReader = Json.createReader(new StringReader(json))) {
      return jReader.readObject();
    }
  }

  /**
   * Calculates a date (current date + fudge) and replaces the JSON entry of 'year-month-day' with
   * the calculated date. If the JSON input does not contain the 'year-month-day' entry, the
   * unmodified input JSON is returned.
   *
   * @param jsonString The JSON string to operate on.
   * @param fudge A random number between 90 and 200.
   * @return The updated JSON string input or the unmodified JSON input if it does not contain the
   *     expected 'year-month-day'
   */
  public static String calculateOccasionDate(String jsonString, int fudge) {
    if (!jsonString.contains(KEY_OCCASION_DATE)) {
      return jsonString;
    }

    String occasion = jsonString;
    Calendar currentCalendar = new GregorianCalendar();
    currentCalendar.add(Calendar.DAY_OF_YEAR, fudge);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String formattedDate = dateFormat.format(currentCalendar.getTime());
    occasion = occasion.replace(KEY_OCCASION_DATE, formattedDate);

    return occasion;
  }
}
