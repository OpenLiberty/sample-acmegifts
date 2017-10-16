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
import java.io.StringReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.wasdev.samples.microProfile.occasions.Occasion.Contribution;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Orchestrator {
  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  private static final String clazz = Orchestrator.class.getName();
  private static final Logger logger = Logger.getLogger(clazz);

  // Group keys
  private static final String JSON_KEY_GROUP_NAME = "name";

  // User keys
  public static final String JSON_KEY_USER_FIRST_NAME = "firstName";
  public static final String JSON_KEY_USER_LAST_NAME = "lastName";
  public static final String JSON_KEY_USER_TWITTER_HANDLE = "twitterHandle";
  public static final String JSON_KEY_USER_WISH_LIST_LINK = "wishListLink";

  // Notification keys
  public static final String JSON_KEY_TWITTER_HANDLE = "twiterHandle";
  public static final String JSON_KEY_MESSAGE = "message";
  public static final String JSON_KEY_TWITTER_NOTIF_MODE = "notificationMode";
  public static final String JSON_KEY_TWITTER_NOTIF_MODE_DM = "directMessage";
  public static final String JSON_KEY_TWITTER_NOTIF_MODE_POST_MENTION = "mention";

  // URLs
  private static String GROUP_SERVICE_URL;
  private static String USER_SERVICE_URL;
  private static String NOTIFICATION_SERVICE_URL;
  private static String NOTIFICATION_1_1_SERVICE_URL;

  Map<String, Future<?>> scheduledOccasions = new HashMap<String, Future<?>>();

  /** Local reference to the occasion resource that we can use to modify the occasion database. */
  private OccasionResource occasionResource = null;

  @Inject private NotificationRetryBean notificationRetryBean;

  @Inject private JwtBuilder jwtBuilder;

  @Inject
  @ConfigProperty(name = "user.service.hostname")
  private String userServiceHostname;

  @Inject
  @ConfigProperty(name = "user.service.port")
  private String userServicePort;

  @Inject
  @ConfigProperty(name = "group.service.hostname")
  private String groupServiceHostname;

  @Inject
  @ConfigProperty(name = "group.service.port")
  private String groupServicePort;

  @Inject
  @ConfigProperty(name = "notification.service.hostname")
  private String notificationServiceHostname;

  @Inject
  @ConfigProperty(name = "notification.service.port")
  private String notificationServicePort;

  @Inject
  @ConfigProperty(name = "notification_1_1.service.hostname")
  private String notification_1_1ServiceHostname;

  @Inject
  @ConfigProperty(name = "notification_1_1.service.port")
  private String notification_1_1ServicePort;

  /* following for jmock unit tests */
  public void setNotificationRetryBean(NotificationRetryBean notificationRetryBean) {
    this.notificationRetryBean = notificationRetryBean;
  }

  /* following for jmock unit tests */
  public void setJwtBuilder(JwtBuilder builder) {
    this.jwtBuilder = builder;
  }

  public void scheduleOccasion(Occasion occasion) throws ParseException {
    final String method = "scheduleOccasion";
    logger.entering(clazz, method, occasion);

    final Runnable notify =
        new Runnable() {

          @Override
          public void run() {
            runEventNotification(occasion);
          }
        };

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Set our occasion trigger time to 8AM
    Date desiredDate = simpleDateFormat.parse(occasion.getDate() + " 08:00:00");
    Date now = new Date();
    long delay = desiredDate.getTime() - now.getTime();

    // Schedule the occasion and save the returned Future
    // TODO: Serialize put into scheduledOccasions to cope with running immediately.
    Future<?> scheduledOccasion =
        executor.schedule(notify, Math.max(delay, 0), TimeUnit.MILLISECONDS);
    String idString = occasion.getId().toString();
    scheduledOccasions.put(idString, scheduledOccasion);

    logger.log(Level.FINE, "Stored future for occasion ID", idString);

    logger.exiting(clazz, method);
  }

  public void cancelOccasion(String occasionId) {
    final String method = "cancelOccasion";
    logger.entering(clazz, method, occasionId);
    Future<?> scheduledOccasion = scheduledOccasions.remove(occasionId);
    logger.log(Level.FINEST, "Retrieved future", scheduledOccasion);
    scheduledOccasion.cancel(false);
    logger.exiting(clazz, method);
  }

  /**
   * Retrieve information from each user and create an email list to send to the notification
   * service.
   *
   * @param payload The JSON payload
   */
  public void runEventNotification(Occasion occasion) {
    notify(occasion);
  }

  private OccasionResponse notify(Occasion occasion) {
    OccasionResponse occasionResponse = null;

    // Generate a JWT that we'll use to talk to the other services.
    // TODO: Move this to the authentication service and secure the flow with certificate auth.
    String jwtTokenString = jwtBuilder.buildCompactJWT("orchestrator", "INTERNAL-ORCHESTRATOR");

    // Call group service to get group name
    Response groupResponse = null;
    try {
      groupResponse =
          makeConnection(
              "GET", GROUP_SERVICE_URL + "/" + occasion.getGroupId(), null, jwtTokenString);
    } catch (IOException e) {
      e.printStackTrace();
    }

    JsonObject groupResponseJson = stringToJsonObj(groupResponse.readEntity(String.class));
    groupResponse.close();

    String groupName = groupResponseJson.getString(JSON_KEY_GROUP_NAME);

    // Call user service to get recipient information
    Response userResponse = null;
    try {
      userResponse =
          makeConnection(
              "GET", USER_SERVICE_URL + "/" + occasion.getRecipientId(), null, jwtTokenString);
    } catch (IOException e) {
      e.printStackTrace();
    }

    JsonObject recipient = stringToJsonObj(userResponse.readEntity(String.class));
    userResponse.close();
    String firstName = recipient.getString(JSON_KEY_USER_FIRST_NAME);
    String lastName = recipient.getString(JSON_KEY_USER_LAST_NAME);
    String twitterHandle = recipient.getString(JSON_KEY_USER_TWITTER_HANDLE);
    String wishList = recipient.getString(JSON_KEY_USER_WISH_LIST_LINK);

    // Add up the total contributions
    double totalAmount = 0;

    for (Contribution contribution : occasion.getContributions()) {
      double contributionAmount = contribution.getAmount();
      totalAmount = totalAmount + contributionAmount;
    }

    // Create notification message to send to the notification service
    String message =
        createEventNotificationMessage(
            firstName,
            lastName,
            wishList,
            groupName,
            occasion.getName(),
            NumberFormat.getCurrencyInstance(Locale.US).format(totalAmount));

    try {
      occasionResponse =
          notificationRetryBean.makeNotificationConnection(
              message,
              this,
              jwtTokenString,
              NOTIFICATION_1_1_SERVICE_URL,
              twitterHandle,
              NOTIFICATION_SERVICE_URL);
    } catch (Throwable t) {
      t.printStackTrace();
      occasionResponse = new OccasionResponse(null, OccasionResponse.NOTIFICATION_TYPE_ERROR, t);
    }

    if (occasionResource != null) {
      occasionResource.deleteOccasion(occasion.getId());
    }
    return occasionResponse;
  }

  /*
   * Make an HTTP connection to the specified URL and pass in the specified payload.
   */
  public Response makeConnection(
      String method, String urlString, String payload, String jwtTokenString) throws IOException {

    Client client = ClientBuilder.newClient();

    // Set the client connection timeout to 10 seconds. This is the amount of time the client
    // will attempt to establish a connection before it times out.
    client.property("http.connection.timeout", 10000);

    WebTarget target = client.target(urlString);
    Response response;

    if (payload != null) {
      // Send JSON payload
      Invocation.Builder invoBuild = target.request(MediaType.APPLICATION_JSON_TYPE);
      Entity<String> data = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
      response =
          invoBuild
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenString)
              .build(method, data)
              .invoke();

    } else {
      // No JSON payload to send
      Invocation.Builder invoBuild = target.request();
      response =
          invoBuild
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenString)
              .build(method)
              .invoke();
    }

    return response;
  }

  public String createEventNotificationMessage(
      String recipientFirstName,
      String recipientLastName,
      String wishList,
      String groupName,
      String occasionName,
      String totalAmount) {
    return "Congratulations "
        + recipientFirstName
        + " "
        + recipientLastName
        + "! "
        + totalAmount
        + " has been contributed by "
        + groupName
        + " for "
        + occasionName
        + ".  Please select a gift from your wish list at "
        + wishList
        + ".";
  }

  private JsonObject stringToJsonObj(String input) {
    try {
      JsonReader jsonReader = Json.createReader(new StringReader(input));
      JsonObject output = jsonReader.readObject();
      jsonReader.close();
      return output;
    } catch (JsonParsingException e) {
      return null;
    }
  }

  public void setOccasionResource(OccasionResource occasionResource) {
    this.occasionResource = occasionResource;
    GROUP_SERVICE_URL = "https://" + groupServiceHostname + ":" + groupServicePort + "/groups";
    USER_SERVICE_URL = "https://" + userServiceHostname + ":" + userServicePort + "/users";
    NOTIFICATION_SERVICE_URL =
        "http://" + notificationServiceHostname + ":" + notificationServicePort + "/notifications";
    NOTIFICATION_1_1_SERVICE_URL =
        "http://"
            + notification_1_1ServiceHostname
            + ":"
            + notification_1_1ServicePort
            + "/notifications";
  }

  public OccasionResponse runOccasion(Occasion occasion) {

    final String method = "runOccasion";
    logger.entering(clazz, method, occasion);

    // cancel occasion since we are going to run it now
    cancelOccasion(occasion.getId().toString());
    OccasionResponse occasionResponse = notify(occasion);

    logger.exiting(clazz, method);
    return occasionResponse;
  }
}
