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
package net.wasdev.samples.microProfile.occasions;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class NotificationRetryBean {

  // Notification keys
  public static final String JSON_KEY_NOTIFICATION = "notification";

  /*
   * Retried twice in the event of failure, after which the fallback handler
   * will be driven to try microservice-notification_v1_1
   */
  @Retry(maxRetries = 2)
  @Fallback(NotificationFallbackHandler.class)
  public OccasionResponse makeNotificationConnection(
      String message,
      Orchestrator orchestrator,
      String jwtTokenString,
      String notification11ServiceUrl,
      String twitterHandle,
      String notificationServiceUrl)
      throws IOException {

    JsonBuilderFactory factory = Json.createBuilderFactory(null);
    JsonObjectBuilder builder = factory.createObjectBuilder();
    JsonObject notificationRequestPayload = builder.add(JSON_KEY_NOTIFICATION, message).build();
    Response notificationResponse =
        orchestrator.makeConnection(
            "POST", notificationServiceUrl, notificationRequestPayload.toString(), jwtTokenString);
    OccasionResponse occasionResponse =
        new OccasionResponse(notificationResponse, OccasionResponse.NOTIFICATION_TYPE_LOG, null);

    return occasionResponse;
  }
}
