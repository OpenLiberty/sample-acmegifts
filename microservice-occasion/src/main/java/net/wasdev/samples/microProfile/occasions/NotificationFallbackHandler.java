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
import javax.enterprise.context.Dependent;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

@Dependent
public class NotificationFallbackHandler implements FallbackHandler<OccasionResponse> {

  private static final String JSON_KEY_NOTIFICATION = "notification";
  private static final String JSON_KEY_TWITTER_HANDLE = "twiterHandle";
  private static final String JSON_KEY_MESSAGE = "message";
  private static final String JSON_KEY_TWITTER_NOTIF_MODE = "notificationMode";

  private static final String JSON_KEY_TWITTER_NOTIF_MODE_POST_MENTION = "mention";

  @Override
  public OccasionResponse handle(ExecutionContext context) {

    Object[] connectParameters = context.getParameters();
    String message = (String) connectParameters[0];
    Orchestrator orchestrator = (Orchestrator) connectParameters[1];
    String jwtTokenString = (String) connectParameters[2];
    String notification11ServiceUrl = (String) connectParameters[3];
    String twitterRecepientHandle = (String) connectParameters[4];

    JsonObjectBuilder content = Json.createObjectBuilder();
    content.add(JSON_KEY_TWITTER_HANDLE, twitterRecepientHandle);
    content.add(JSON_KEY_TWITTER_NOTIF_MODE, JSON_KEY_TWITTER_NOTIF_MODE_POST_MENTION);
    content.add(JSON_KEY_MESSAGE, message);
    JsonObjectBuilder notification = Json.createObjectBuilder();
    notification.add(JSON_KEY_NOTIFICATION, content.build());
    String payload = notification.build().toString();

    Response notificationResponse = null;
    try {
      notificationResponse =
          orchestrator.makeConnection("POST", notification11ServiceUrl, payload, jwtTokenString);
    } catch (IOException e) {
      e.printStackTrace();
    }
    OccasionResponse occasionResponse =
        new OccasionResponse(notificationResponse, OccasionResponse.NOTIFICATION_TYPE_TWEET, null);
    return occasionResponse;
  }
}
