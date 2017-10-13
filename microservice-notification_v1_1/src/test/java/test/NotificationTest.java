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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
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

/** Notification Resource tests. */
public class NotificationTest {
  /**
   * The twitter handle (AKA ID/UserName) of the user you wish to receive notifications. It must be
   * a valid/active twitter handle.
   */
  private final String twitterRecepientHandle = "CHANGE_ME";

  /**
   * The name of the twitter user associated with the handle shown under twitterRecepientHandle. It
   * can be anything.
   */
  private final String twitterRecepientName = "CHANGE_ME";

  private static final String libertyPort = System.getProperty("liberty.test.port");
  private static final String libertyHostname = System.getProperty("liberty.test.hostname");
  private static final String logFile = System.getProperty("log.file");
  private static final String fallbackLogFile = System.getProperty("fallback.log.file");
  private static final String notificationServiceURL =
      "http://" + libertyHostname + ":" + libertyPort + "/notifications";

  public static final String JSON_KEY_NOTIFICATION = "notification";
  public static final String JSON_KEY_TWITTER_HANDLE = "twiterHandle";
  public static final String JSON_KEY_MESSAGE = "message";

  /**
   * Tests sending a twitter post and a direct notification. IMPORTANT: Before enabling this test,
   * update the application's twitter related properties (default: CHANGE_ME) with valid values.
   * These properties are located in the application's root pom.xml.
   */
  // @Test
  public void testTwitterPostMention() throws Exception {
    // Post a message.
    String userMention = "@" + twitterRecepientHandle;
    JsonObjectBuilder content = Json.createObjectBuilder();
    content.add(JSON_KEY_TWITTER_HANDLE, twitterRecepientHandle);
    content.add(
        JSON_KEY_MESSAGE,
        "Merry Christmas "
            + userMention
            + "."
            + System.getProperty("line.separator")
            + "Jack D., Jane D. and James D. contributed $10,000 for your gift. A wishlist gift was ordered and mailed.");
    JsonObjectBuilder notification = Json.createObjectBuilder();
    notification.add(JSON_KEY_NOTIFICATION, content.build());

    Response response =
        processRequest(notificationServiceURL, "POST", notification.build().toString());
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Verify that the notification was logged.
    BufferedReader br = new BufferedReader(new FileReader(logFile));
    try {
      String line = null;
      while ((line = br.readLine()) != null) {
        if (line.contains("Merry Christmas " + userMention)) {
          return;
        }
      }

      fail("Not all notifications were found.");
    } finally {
      br.close();
    }
  }

  /** Tests a twitter notification with an invalid recepient handle. */
  @Test
  public void testInvalidTwitterRecepientHandle() throws Exception {
    // Request a twitter notification with an invalid mode.
    JsonObjectBuilder content = Json.createObjectBuilder();
    content.add(JSON_KEY_TWITTER_HANDLE, "BAD_RECEPIENT_HANDLE");
    content.add(
        JSON_KEY_MESSAGE,
        "Happy New Year "
            + twitterRecepientName
            + System.getProperty("line.separator")
            + "Steve W., Don W., and Jane W. have contributed a total of $100,000 for your gift. A gift from your wishlist at link: http://www.somewishlistLink/Sarah was ordered and mailed.");
    JsonObjectBuilder notification = Json.createObjectBuilder();
    notification.add(JSON_KEY_NOTIFICATION, content.build());
    Response response =
        processRequest(notificationServiceURL, "POST", notification.build().toString());
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Verify that the notification was logged.
    BufferedReader br = new BufferedReader(new FileReader(logFile));
    boolean foundMsg = false;
    try {
      String line = null;
      while ((line = br.readLine()) != null) {
        if (line.contains("Happy New Year " + twitterRecepientName)) {
          foundMsg = true;
          break;
        }
      }

      if (foundMsg == false) {
        fail("Not all notifications were found.");
      }
    } finally {
      br.close();
    }

    // Verify that the fallback notification was logged.
    BufferedReader fbBr = new BufferedReader(new FileReader(fallbackLogFile));
    try {
      String line = null;
      while ((line = fbBr.readLine()) != null) {
        if (line.contains("Happy New Year " + twitterRecepientName)) {
          return;
        }
      }

      fail("Not all fallback notifications were found.");
    } finally {
      fbBr.close();
    }
  }

  public Response processRequest(String url, String method, String payload)
      throws GeneralSecurityException, IOException {
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(url);
    Builder builder = target.request();
    builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    builder.header(
        HttpHeaders.AUTHORIZATION,
        "Bearer "
            + new JWTVerifier()
                .createJWT("fred", new HashSet<String>(Arrays.asList("orchestrator"))));
    return (payload != null)
        ? builder.build(method, Entity.json(payload)).invoke()
        : builder.build(method).invoke();
  }
}
