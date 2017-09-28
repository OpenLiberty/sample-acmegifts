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
package unit_test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import net.wasdev.samples.microProfile.occasions.JwtBuilder;
import net.wasdev.samples.microProfile.occasions.NotificationRetryBean;
import net.wasdev.samples.microProfile.occasions.Occasion;
import net.wasdev.samples.microProfile.occasions.Occasion.Contribution;
import net.wasdev.samples.microProfile.occasions.OccasionResource;
import net.wasdev.samples.microProfile.occasions.Orchestrator;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mockito.Mockito;

// ------------------------------------------------------------------------
// ------------------------------------------------------------------------
//   ******   *******     *     ******
//   **   **  **         ***    **   **
//   ******   *****     ** **   **   **
//   **  **   **       *******  **   **
//   **   **  *******  **   **  ******
//
// It is very important the the URLs that are mocked/spied in this test
// are correct, or else the test will fail with an exception that is very
// hard to diagnose.
// ------------------------------------------------------------------------
// ------------------------------------------------------------------------
public class OrchestratorResourceMockTest {

  // Group keys
  private static final String JSON_KEY_GROUP_NAME = "name";
  private static final String JSON_KEY_MEMBERS_LIST = "members";
  private static final String JSON_KEY_OCCASIONS_LIST = "occasions";

  // User keys
  public static final String JSON_KEY_USER_FIRST_NAME = "firstName";
  public static final String JSON_KEY_USER_LAST_NAME = "lastName";
  public static final String JSON_KEY_USER_NAME = "userName";
  public static final String JSON_KEY_USER_TWITTER_HANDLE = "twitterHandle";
  public static final String JSON_KEY_USER_WISH_LIST_LINK = "wishListLink";
  public static final String JSON_KEY_USER_GROUPS = "groups";

  // Notification keys
  private static final String JSON_KEY_NOTIFICATION = "notification";

  private static final String GROUP_ID = "group123";
  private static final String USER_ID = "user123";
  private static final String GROUP_SERVICE_URL = "https://null:null/groups/" + GROUP_ID;
  private static final String USER_SERVICE_URL = "https://null:null/users/" + USER_ID;
  private static final String NOTIFICATION_SERVICE_URL = "http://null:null/notifications";

  @Test
  public void testEventNotification_OneContribution() throws IOException {
    System.out.println("\nStarting testEventNotification_OneUser");

    // Dummy data
    String groupId = GROUP_ID;
    String groupName = "Friends";

    String userId = USER_ID;
    String userFirstName = "Jane";
    String userLastName = "Doe";
    String userName = "JD";
    String userTwitterHandle = "JDTwitter";
    String userWishListLink = "myWishList.com";

    String name = "Jane's Birthday";
    double contribution = 30;
    Occasion occasion = generateOccasion(name, groupId, userId, new double[] {contribution});

    String emailContent = "You got an email";

    // Initialize an instance of OrchestratorResource
    OccasionResource occasionResource = Mockito.mock(OccasionResource.class);
    JwtBuilder jwtBuilder = Mockito.mock(JwtBuilder.class);
    NotificationRetryBean notificationRetryBean = new NotificationRetryBean();
    Orchestrator orchestrator = new Orchestrator();
    orchestrator.setOccasionResource(occasionResource);
    orchestrator.setNotificationRetryBean(notificationRetryBean);
    orchestrator.setJwtBuilder(jwtBuilder);

    // Setup mocked JSON payloads
    String groupResponseJson =
        buildGroupResponseObject(
            groupName, new String[] {userId}, new String[] {occasion.getId().toString()});
    Response groupResponse = Response.ok().entity(groupResponseJson).build();

    String userResponseJson =
        buildUserResponseObject(
            userFirstName,
            userLastName,
            userName,
            userTwitterHandle,
            userWishListLink,
            new String[] {groupId});
    Response userResponse = Response.ok().entity(userResponseJson).build();

    String notificationRequestJson = buildNotificationRequestObject(emailContent);

    // Setup expected return values for mocked methods
    String jwtString = "JWT";
    Orchestrator mock_orchestrator = Mockito.spy(orchestrator);
    doReturn(groupResponse)
        .when(mock_orchestrator)
        .makeConnection("GET", GROUP_SERVICE_URL, null, jwtString);
    doReturn(userResponse)
        .when(mock_orchestrator)
        .makeConnection("GET", USER_SERVICE_URL, null, jwtString);
    doReturn(emailContent)
        .when(mock_orchestrator)
        .createEventNotificationMessage(
            userFirstName,
            userLastName,
            userWishListLink,
            groupName,
            name,
            NumberFormat.getCurrencyInstance(Locale.US).format(contribution));
    doReturn(Response.ok().build())
        .when(mock_orchestrator)
        .makeConnection(eq("POST"), eq(NOTIFICATION_SERVICE_URL), any(String.class), eq(jwtString));
    doReturn(Boolean.TRUE).when(occasionResource).deleteOccasion(occasion.getId());
    doReturn(jwtString).when(jwtBuilder).buildCompactJWT(any(String.class), any(String.class));

    // Make orchestrator call
    try {
      mock_orchestrator.runEventNotification(occasion);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    }

    // Setup verifications to make sure the mocked methods are called with
    // the expected parameters
    verify(jwtBuilder, times(1)).buildCompactJWT("orchestrator", "INTERNAL-ORCHESTRATOR");
    verify(mock_orchestrator, times(1)).makeConnection("GET", GROUP_SERVICE_URL, null, jwtString);
    verify(mock_orchestrator, times(1)).makeConnection("GET", USER_SERVICE_URL, null, jwtString);
    verify(mock_orchestrator, times(1))
        .createEventNotificationMessage(
            userFirstName,
            userLastName,
            userWishListLink,
            groupName,
            name,
            NumberFormat.getCurrencyInstance(Locale.US).format(contribution));
    verify(mock_orchestrator, times(1))
        .makeConnection("POST", NOTIFICATION_SERVICE_URL, notificationRequestJson, jwtString);
    verify(occasionResource, times(1)).deleteOccasion(occasion.getId());
  }

  @Test
  public void testEventNotification_TwoContributions() throws IOException {
    System.out.println("\nStarting testEventNotification_TwoUsers");

    // Dummy data
    String groupId = GROUP_ID;
    String groupName = "Friends";

    String user1Id = USER_ID;
    String user1FirstName = "Jane";
    String user1LastName = "Doe";
    String user1Name = "JaneD";
    String user1Email = "jane.doe@myMail.com";
    String user1WishListLink = "myWishList.com";

    String user2Id = "user456";

    double user1ContributionAmount = 30;
    double user2ContributionAmount = 20;

    String name = "Jane's Birthday";
    Occasion occasion =
        generateOccasion(
            name,
            groupId,
            user1Id,
            new double[] {user1ContributionAmount, user2ContributionAmount});
    double totalContributionAmount = 50;

    String emailContent = "You got an email";

    // Initialize an instance of OrchestratorResource
    OccasionResource occasionResource = Mockito.mock(OccasionResource.class);
    NotificationRetryBean notificationRetryBean = new NotificationRetryBean();
    JwtBuilder jwtBuilder = Mockito.mock(JwtBuilder.class);
    Orchestrator orchestrator = new Orchestrator();
    orchestrator.setOccasionResource(occasionResource);
    orchestrator.setNotificationRetryBean(notificationRetryBean);
    orchestrator.setJwtBuilder(jwtBuilder);

    // Setup mocked JSON payloads
    String groupResponseJson =
        buildGroupResponseObject(
            groupName, new String[] {user1Id, user2Id}, new String[] {occasion.getId().toString()});
    Response groupResponse = Response.ok().entity(groupResponseJson).build();

    String user1ResponseJson =
        buildUserResponseObject(
            user1FirstName,
            user1LastName,
            user1Name,
            user1Email,
            user1WishListLink,
            new String[] {groupId});
    Response user1Response = Response.ok().entity(user1ResponseJson).build();

    String notificationRequestJson = buildNotificationRequestObject(emailContent);

    // Setup expected return values for mocked methods
    String jwtString = "JWT";
    Orchestrator mock_orchestrator = Mockito.spy(orchestrator);
    doReturn(groupResponse)
        .when(mock_orchestrator)
        .makeConnection("GET", GROUP_SERVICE_URL, null, jwtString);
    doReturn(user1Response)
        .when(mock_orchestrator)
        .makeConnection("GET", USER_SERVICE_URL, null, jwtString);
    doReturn(emailContent)
        .when(mock_orchestrator)
        .createEventNotificationMessage(
            user1FirstName,
            user1LastName,
            user1WishListLink,
            groupName,
            name,
            NumberFormat.getCurrencyInstance(Locale.US).format(totalContributionAmount));
    doReturn(Response.ok().build())
        .when(mock_orchestrator)
        .makeConnection(eq("POST"), eq(NOTIFICATION_SERVICE_URL), any(String.class), eq(jwtString));
    doReturn(Boolean.TRUE).when(occasionResource).deleteOccasion(occasion.getId());
    doReturn(jwtString).when(jwtBuilder).buildCompactJWT(any(String.class), any(String.class));

    // Make orchestrator call
    mock_orchestrator.runEventNotification(occasion);

    // Setup verifications to make sure the mocked methods are called with
    // the expected parameters
    verify(jwtBuilder, times(1)).buildCompactJWT("orchestrator", "INTERNAL-ORCHESTRATOR");
    verify(mock_orchestrator, times(1)).makeConnection("GET", GROUP_SERVICE_URL, null, jwtString);
    verify(mock_orchestrator, times(1)).makeConnection("GET", USER_SERVICE_URL, null, jwtString);
    verify(mock_orchestrator, times(1))
        .createEventNotificationMessage(
            user1FirstName,
            user1LastName,
            user1WishListLink,
            groupName,
            name,
            NumberFormat.getCurrencyInstance(Locale.US).format(totalContributionAmount));
    verify(mock_orchestrator, times(1))
        .makeConnection("POST", NOTIFICATION_SERVICE_URL, notificationRequestJson, jwtString);
    verify(occasionResource, times(1)).deleteOccasion(occasion.getId());
  }

  private String buildGroupResponseObject(String name, String[] members, String[] occasions) {
    JsonObjectBuilder group = Json.createObjectBuilder();
    group.add(JSON_KEY_GROUP_NAME, name);

    JsonArrayBuilder membersArray = Json.createArrayBuilder();
    for (int i = 0; i < members.length; i++) {
      membersArray.add(members[i]);
    }
    group.add(JSON_KEY_MEMBERS_LIST, membersArray.build());

    JsonArrayBuilder occasionsArray = Json.createArrayBuilder();
    for (int i = 0; i < occasions.length; i++) {
      occasionsArray.add(occasions[i]);
    }
    group.add(JSON_KEY_OCCASIONS_LIST, occasionsArray.build());

    return group.build().toString();
  }

  private String buildUserResponseObject(
      String firstName,
      String lastName,
      String userName,
      String twitterHandle,
      String wishListLink,
      String[] groups) {
    JsonObjectBuilder user = Json.createObjectBuilder();
    user.add(JSON_KEY_USER_FIRST_NAME, firstName);
    user.add(JSON_KEY_USER_LAST_NAME, lastName);
    user.add(JSON_KEY_USER_NAME, userName);
    user.add(JSON_KEY_USER_TWITTER_HANDLE, twitterHandle);
    user.add(JSON_KEY_USER_WISH_LIST_LINK, wishListLink);

    JsonArrayBuilder groupArray = Json.createArrayBuilder();
    for (int i = 0; i < groups.length; i++) {
      groupArray.add(groups[i]);
    }
    user.add(JSON_KEY_USER_GROUPS, groupArray.build());

    return user.build().toString();
  }

  private String buildNotificationRequestObject(String notification) {

    JsonObjectBuilder notificationRequestPayload = Json.createObjectBuilder();
    notificationRequestPayload.add(JSON_KEY_NOTIFICATION, notification);
    return notificationRequestPayload.build().toString();
  }

  /** Build a dummy occasion. */
  private Occasion generateOccasion(
      String name, String groupId, String recipientId, double[] contributionAmounts) {
    ObjectId id = new ObjectId();
    List<Contribution> contributionList = new LinkedList<Contribution>();
    for (int x = 0; x < contributionAmounts.length; x++) {
      contributionList.add(new Contribution(Integer.toString(x), contributionAmounts[x]));
    }
    return new Occasion(
        id,
        null, /* String date */
        groupId, /* String groupId */
        null, /* String interval */
        name,
        null, /* Organizer ID */
        recipientId, /* Recipient ID */
        contributionList);
  }
}
