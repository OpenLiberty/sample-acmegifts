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

  private static final String libertyHostname = System.getProperty("liberty.test.hostname");
  private static final String libertyPort = System.getProperty("liberty.test.port");
  private static final String logFile = System.getProperty("log.file");
  private static final String notificationServiceURL =
      "http://" + libertyHostname + ":" + libertyPort + "/notifications";
  private static final String JSON_KEY_NOTIFICATION = "notification";

  /** Tests sending a single notification. */
  @Test
  public void testNotification() throws Exception {
    JsonObjectBuilder payload = Json.createObjectBuilder();
    payload.add(
        JSON_KEY_NOTIFICATION,
        "Happy birthday Joe D.\nJack D., Jane D. and James D. have contributed a total of $10,000 for your gift. A gift from your wishlist at link: http://www.somewishlistLink/Joe was ordered and mailed.");

    Response response = processRequest(notificationServiceURL, "POST", payload.build().toString());
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    payload = Json.createObjectBuilder();
    payload.add(
        JSON_KEY_NOTIFICATION,
        "Happy Mother's Day Sarah W.\nSteve W., Don W. and Jane W. have contributed a total of $100,000 for your gift. A gift from your wishlist at link: http://www.somewishlistLink/Sarah was ordered and mailed.");

    response = processRequest(notificationServiceURL, "POST", payload.build().toString());
    assertEquals(
        "HTTP response code should have been " + Status.OK.getStatusCode() + ".",
        Status.OK.getStatusCode(),
        response.getStatus());

    // Verify that the two notifications sent were logged.
    BufferedReader br = new BufferedReader(new FileReader(logFile));
    try {
      String line = null;
      boolean notification1Found = false;
      boolean notification2Found = false;
      while ((line = br.readLine()) != null) {
        if (line.contains("Happy birthday Joe D.")) {
          notification1Found = true;
        }
        if (line.contains("Happy Mother's Day Sarah W.")) {
          notification2Found = true;
        }
      }

      assertTrue(
          "Not all notifications were found. Found: notification1: "
              + notification1Found
              + "notification2: "
              + notification2Found,
          (notification1Found && notification2Found));
    } finally {
      br.close();
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
