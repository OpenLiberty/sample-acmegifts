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

import javax.ws.rs.core.Response;

public class OccasionResponse {
  public static final String NOTIFICATION_TYPE_LOG = "Notification request logged.";
  public static final String NOTIFICATION_TYPE_TWEET = "Notification request tweeted.";
  public static final String NOTIFICATION_TYPE_ERROR =
      "Your occasion was processed but a notification request was not sent. The notification service is not available.";

  private Response notificationResponse;
  private String notificationType;
  private Throwable notificationThrowable;

  public OccasionResponse(Response response, String type, Throwable throwable) {
    notificationResponse = response;
    notificationType = type;
    notificationThrowable = throwable;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public Response getNotificationResponse() {
    return notificationResponse;
  }

  public Throwable getNotificationThrowable() {
    return notificationThrowable;
  }
}
