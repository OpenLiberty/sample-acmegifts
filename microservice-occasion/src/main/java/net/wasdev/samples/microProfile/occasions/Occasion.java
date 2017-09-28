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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import org.apache.commons.collections4.ListUtils;
import org.bson.types.ObjectId;

/** A class representing and Occasion and some helper methods */
public class Occasion {
  /*
     Example object
     {
        <implied object id>,
        groupid: 0001
        occasionname: "John Doe's Birthday",
        date: "2017-10-25",
        interval: annual
        organizerId: 0001
        recipientId: 0004
        contributions: [
                   {
                     userid: 0001,
                     amount: 20 (dollars)
                   },
                   {
                     userid: 0002,
                     amount: 50
                   },
                   {
                     userid: 0003,
                     amount: 50
                   }
                 ]
     }
  */

  private static final String clazz = Occasion.class.getName();
  private static final Logger logger = Logger.getLogger(clazz);

  /*
   * Occasion Keys
   */
  public static final String OCCASION_ID_KEY = "_id";
  public static final String OCCASION_DATE_KEY = "date";
  public static final String OCCASION_GROUP_ID_KEY = "groupId";
  public static final String OCCASION_INTERVAL_KEY = "interval";
  public static final String OCCASION_NAME_KEY = "name";
  public static final String OCCASION_ORGANIZER_ID_KEY = "organizerId";
  public static final String OCCASION_RECIPIENT_ID_KEY = "recipientId";
  public static final String OCCASION_CONTRIBUTIONS_KEY = "contributions";
  public static final String OCCASION_CONTRIBUTION_AMOUNT_KEY = "amount";
  public static final String OCCASION_CONTRIBUTION_USER_ID_KEY = "userId";

  /*
   * Occasion Values
   */
  private ObjectId id;
  private String date;
  private String groupId;
  private String interval;
  private String name;
  private String organizerId;
  private String recipientId;
  private List<Contribution> contributions;

  public static class Contribution {
    private String contributionUserId;
    private double amount;

    /*
     * Static list helpers
     */
    public static JsonArray listToJsonArray(List<Contribution> contributions) {
      String method = "listToJsonArray";
      logger.entering(clazz, method);

      JsonArrayBuilder contributionsBuilder = Json.createArrayBuilder();
      for (Contribution contribution : ListUtils.emptyIfNull(contributions)) {
        contributionsBuilder.add(contribution.toJson());
      }
      JsonArray contributionsOut = contributionsBuilder.build();

      logger.exiting(clazz, method, contributionsOut);
      return contributionsOut;
    }

    public static BasicDBList listToDBList(List<Contribution> contributions) {
      String method = "listToString";
      logger.entering(clazz, method);
      BasicDBList dbl = new BasicDBList();
      for (Contribution contribution : ListUtils.emptyIfNull(contributions)) {
        dbl.add(contribution.toDbo());
      }
      logger.exiting(clazz, method);
      return dbl;
    }

    public static String listToString(List<Contribution> contributions) {
      String method = "listToString";
      logger.entering(clazz, method);
      StringBuilder sb = new StringBuilder();
      for (Contribution contribution : ListUtils.emptyIfNull(contributions)) {
        sb.append(contribution.toString());
      }
      String str = sb.toString();
      logger.exiting(clazz, method, str);
      return str;
    }

    public static List<Contribution> jsonArrayToList(JsonArray jsonArray) {
      String method = "jsonArrayToList";
      logger.entering(clazz, method, jsonArray);

      List<Contribution> contributions = new ArrayList<>();
      for (JsonValue json : jsonArray) {
        contributions.add(new Contribution((JsonObject) json));
      }

      logger.exiting(clazz, method, listToString(contributions));
      return contributions;
    }

    public static List<Contribution> dbListToList(BasicDBList dbl) {
      String method = "dbListToList";
      logger.entering(clazz, method, dbl);

      List<Contribution> contributions = new ArrayList<>();
      for (Object dbo : ListUtils.emptyIfNull(dbl)) {
        contributions.add(new Contribution((BasicDBObject) dbo));
      }

      logger.exiting(clazz, method, listToString(contributions));
      return contributions;
    }

    public static boolean listsEqual(List<Contribution> list1, List<Contribution> list2) {
      for (Contribution p1 : ListUtils.emptyIfNull(list1)) {
        if (!list2.contains(p1)) return false;
      }
      for (Contribution p2 : ListUtils.emptyIfNull(list2)) {
        if (!list1.contains(p2)) return false;
      }
      return true;
    }

    /*
     * Constructors
     */
    public Contribution(String str) {
      this((DBObject) JSON.parse(str));
    }

    public Contribution(DBObject dbo) {
      String method = "Contribution";
      logger.entering(clazz, method, dbo);

      setUserId((String) dbo.get(OCCASION_CONTRIBUTION_USER_ID_KEY));
      setAmount((Double) dbo.get(OCCASION_CONTRIBUTION_AMOUNT_KEY));

      logger.exiting(clazz, method, "amount: " + getAmount() + ", userId: " + getUserId());
    }

    public Contribution(JsonObject json) {
      String method = "Contribution";
      logger.entering(clazz, method, json);

      setUserId(json.getString(OCCASION_CONTRIBUTION_USER_ID_KEY, null));

      JsonNumber amount = json.getJsonNumber(OCCASION_CONTRIBUTION_AMOUNT_KEY);
      setAmount((amount == null) ? 0 : amount.doubleValue());

      logger.exiting(clazz, method, "amount: " + getAmount() + ", userId: " + getUserId());
    }

    public Contribution(String contributionUserId, double amount) {
      String method = "Contribution";
      logger.entering(clazz, method);

      setUserId(contributionUserId);
      setAmount(amount);

      logger.exiting(clazz, method, "amount: " + getAmount() + ", userId: " + getUserId());
    }

    /*
     * Getters and Setters
     */
    public void setUserId(String contributionUserId) {
      this.contributionUserId = contributionUserId;
    }

    public String getUserId() {
      return contributionUserId;
    }

    public void setAmount(double amount) {
      this.amount = amount;
    }

    public double getAmount() {
      return amount;
    }

    /*
     * Conversion methods
     */
    public BasicDBObject toDbo() {
      return new BasicDBObject(OCCASION_CONTRIBUTION_AMOUNT_KEY, amount)
          .append(OCCASION_CONTRIBUTION_USER_ID_KEY, contributionUserId);
    }

    public JsonObject toJson() {
      return Json.createObjectBuilder()
          .add(OCCASION_CONTRIBUTION_AMOUNT_KEY, amount)
          .add(OCCASION_CONTRIBUTION_USER_ID_KEY, contributionUserId)
          .build();
    }

    @Override
    public String toString() {
      return toJson().toString();
    }

    @Override
    public boolean equals(Object object) {
      Contribution contribution = (Contribution) object;
      if (null == contribution) return false;
      return (getUserId().equals(contribution.getUserId())
          && (getAmount() == contribution.getAmount()));
    }

    @Override
    public int hashCode() {
      return this.toString().length();
    }
  }

  /*
   * Static list helper methods
   */
  public static BasicDBList jsonArrayToDbList(JsonArray jsonArray) {
    String method = "jsonArrayToDbList";
    logger.entering(clazz, method, jsonArray);

    BasicDBList dbl = new BasicDBList();
    for (JsonValue json : ListUtils.emptyIfNull(jsonArray)) {
      BasicDBObject dbo = new Occasion((JsonObject) json).toDbo();
      dbl.add(dbo);
    }

    logger.exiting(clazz, method, dbl);
    return dbl;
  }

  public static JsonArray dboListToJsonArray(List<DBObject> dboList) {
    String method = "dboListToJsonArray";
    logger.entering(clazz, method, dboList);

    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    for (Object dbo : ListUtils.emptyIfNull(dboList)) {
      JsonObject json = new Occasion((DBObject) dbo).toJson();
      arrayBuilder.add(json);
    }
    JsonArray returnArray = arrayBuilder.build();

    logger.exiting(clazz, method, returnArray);
    return returnArray;
  }

  /*
   * Constructors
   */
  public Occasion(String str) {
    this((DBObject) JSON.parse(str));
  }

  public Occasion(JsonObject json) {
    String method = "Occasion";
    logger.entering(clazz, method, json);

    setId(json.getString(OCCASION_ID_KEY, null));
    setDate(json.getString(OCCASION_DATE_KEY, null));
    setGroupId(json.getString(OCCASION_GROUP_ID_KEY, null));
    setInterval(json.getString(OCCASION_INTERVAL_KEY, null));
    setName(json.getString(OCCASION_NAME_KEY, null));
    setOrganizerId(json.getString(OCCASION_ORGANIZER_ID_KEY, null));
    setRecipientId(json.getString(OCCASION_RECIPIENT_ID_KEY, null));
    setContributions(json.get(OCCASION_CONTRIBUTIONS_KEY));

    logger.exiting(clazz, method, this);
  }

  public Occasion(DBObject dbo) {
    String method = "Occasion";
    logger.entering(clazz, method, dbo);

    setId((ObjectId) dbo.get(OCCASION_ID_KEY));
    setDate((String) dbo.get(OCCASION_DATE_KEY));
    setGroupId((String) dbo.get(OCCASION_GROUP_ID_KEY));
    setInterval((String) dbo.get(OCCASION_INTERVAL_KEY));
    setName((String) dbo.get(OCCASION_NAME_KEY));
    setOrganizerId((String) dbo.get(OCCASION_ORGANIZER_ID_KEY));
    setContributions(Contribution.dbListToList((BasicDBList) dbo.get(OCCASION_CONTRIBUTIONS_KEY)));
    setRecipientId((String) dbo.get(OCCASION_RECIPIENT_ID_KEY));

    logger.exiting(clazz, method, this);
  }

  public Occasion(
      ObjectId id,
      String date,
      String groupId,
      String interval,
      String name,
      String organizerId,
      String recipientId,
      List<Contribution> contributions) {

    String method = "Occasion";
    logger.entering(clazz, method);

    logger.fine("id: " + id);
    logger.fine("date: " + date);
    logger.fine("groupId: " + groupId);
    logger.fine("interval: " + interval);
    logger.fine("name: " + name);
    logger.fine("organizerId: " + organizerId);
    logger.fine("recipientId: " + recipientId);
    logger.fine("contributions: " + Contribution.listToString(contributions));

    setId(id);
    setDate(date);
    setGroupId(groupId);
    setInterval(interval);
    setName(name);
    setOrganizerId(organizerId);
    setRecipientId(recipientId);
    setContributions(contributions);

    logger.exiting(clazz, method, this);
  }

  /*
   * Getters and Setters
   */
  public void setId(String id) {
    if (null != id && !id.isEmpty() && ObjectId.isValid(id)) {
      this.id = new ObjectId(id);
    }
  }

  public void setId(ObjectId id) {
    if (null != id) {
      this.id = id;
    }
  }

  public ObjectId getId() {
    return id;
  }

  public void setGroupId(String groupId) {
    if (null == groupId || groupId.isEmpty()) {
      this.groupId = "";
    } else {
      this.groupId = groupId;
    }
  }

  public String getGroupId() {
    return groupId;
  }

  public void setDate(String date) {
    if (null == date || date.isEmpty()) {
      this.date = "";
    } else {
      this.date = date;
    }
  }

  public String getDate() {
    return date;
  }

  public void setInterval(String interval) {
    if (null == interval || interval.isEmpty()) {
      this.interval = "";
    } else {
      this.interval = interval;
    }
  }

  public String getInterval() {
    return interval;
  }

  public void setName(String name) {
    if (null == name || name.isEmpty()) {
      this.name = "";
    } else {
      this.name = name;
    }
  }

  public String getName() {
    return name;
  }

  public void setOrganizerId(String organizerId) {
    if (null == organizerId || organizerId.isEmpty()) {
      this.organizerId = "";
    } else {
      this.organizerId = organizerId;
    }
  }

  public String getOrganizerId() {
    return organizerId;
  }

  public void setContributions(JsonValue contributions) {
    if (null == contributions || contributions.toString().isEmpty()) {
      this.contributions = new ArrayList<Contribution>();
    } else {
      this.contributions = Contribution.jsonArrayToList((JsonArray) contributions);
    }
  }

  public void setContributions(List<Contribution> contributions) {
    if (null == contributions || 1 > contributions.size()) {
      this.contributions = new ArrayList<>();
    } else {
      this.contributions = contributions;
    }
  }

  public List<Contribution> getContributions() {
    return contributions;
  }

  public void setRecipientId(String recipientId) {
    if (null == recipientId || recipientId.isEmpty()) {
      this.recipientId = "";
    } else {
      this.recipientId = recipientId;
    }
  }

  public String getRecipientId() {
    return recipientId;
  }

  /*
   * Conversion methods
   */
  public BasicDBObject toDbo() {
    String method = "toDbo";
    logger.entering(clazz, method);

    logger.fine("id: " + id);
    logger.fine("date: " + date);
    logger.fine("groupId: " + groupId);
    logger.fine("interval: " + interval);
    logger.fine("name: " + name);
    logger.fine("organizerId: " + organizerId);
    logger.fine("recipientId: " + recipientId);
    logger.fine("contributions: " + Contribution.listToString(contributions));

    // build the db object
    BasicDBObject dbo = new BasicDBObject();
    if (null != id && ObjectId.isValid(id.toString())) {
      dbo.append(OCCASION_ID_KEY, id);
    }

    if (null != date && !date.isEmpty()) {
      dbo.append(OCCASION_DATE_KEY, date);
    }

    if (null != groupId && !groupId.isEmpty()) {
      dbo.append(OCCASION_GROUP_ID_KEY, groupId);
    }

    if (null != interval && !interval.isEmpty()) {
      dbo.append(OCCASION_INTERVAL_KEY, interval);
    }

    if (null != name && !name.isEmpty()) {
      dbo.append(OCCASION_NAME_KEY, name);
    }

    if (null != organizerId && !organizerId.isEmpty()) {
      dbo.append(OCCASION_ORGANIZER_ID_KEY, organizerId);
    }

    if (null != recipientId && !recipientId.isEmpty()) {
      dbo.append(OCCASION_RECIPIENT_ID_KEY, recipientId);
    }

    BasicDBList contributionDbList = Contribution.listToDBList(contributions);
    if (null != contributionDbList && !contributionDbList.isEmpty()) {
      dbo.append(OCCASION_CONTRIBUTIONS_KEY, contributionDbList);
    }

    logger.exiting(clazz, method, dbo);
    return dbo;
  }

  public JsonObject toJson() {
    String method = "toJson";
    logger.entering(clazz, method);

    logger.fine("id: " + id);
    logger.fine("date: " + date);
    logger.fine("groupId: " + groupId);
    logger.fine("interval: " + interval);
    logger.fine("name: " + name);
    logger.fine("organizerId: " + organizerId);
    logger.fine("recipientId: " + recipientId);
    logger.fine("contributions: " + Contribution.listToString(contributions));

    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    if (null != id && ObjectId.isValid(id.toString())) {
      jsonBuilder.add(OCCASION_ID_KEY, id.toString());
    }

    if (null != date && !date.isEmpty()) {
      jsonBuilder.add(OCCASION_DATE_KEY, date);
    }

    if (null != groupId && !groupId.isEmpty()) {
      jsonBuilder.add(OCCASION_GROUP_ID_KEY, groupId);
    }

    if (null != interval && !interval.isEmpty()) {
      jsonBuilder.add(OCCASION_INTERVAL_KEY, interval);
    }

    if (null != name && !name.isEmpty()) {
      jsonBuilder.add(OCCASION_NAME_KEY, name);
    }

    if (null != organizerId && !organizerId.isEmpty()) {
      jsonBuilder.add(OCCASION_ORGANIZER_ID_KEY, organizerId);
    }

    if (null != recipientId && !recipientId.isEmpty()) {
      jsonBuilder.add(OCCASION_RECIPIENT_ID_KEY, recipientId);
    }

    JsonArrayBuilder contributionArrayBuilder = Json.createArrayBuilder();
    for (Contribution contribution : ListUtils.emptyIfNull(contributions)) {
      JsonObject contributionJson = contribution.toJson();
      contributionArrayBuilder.add(contributionJson);
    }
    JsonArray contributions = contributionArrayBuilder.build();

    if (null != contributions && !contributions.isEmpty()) {
      jsonBuilder.add(OCCASION_CONTRIBUTIONS_KEY, contributions);
    }

    JsonObject json = jsonBuilder.build();
    logger.exiting(clazz, method, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  @Override
  public boolean equals(Object object) {
    Occasion occasion = (Occasion) object;
    if (null == getId() || null == occasion.getId()) {
      return (getGroupId().equals(occasion.getGroupId())
          && getDate().equals(occasion.getDate())
          && getInterval().equals(occasion.getInterval())
          && getName().equals(occasion.getName())
          && getOrganizerId().equals(occasion.getOrganizerId())
          && getRecipientId().equals(occasion.getRecipientId())
          && Contribution.listsEqual(getContributions(), occasion.getContributions()));
    } else {
      return getId().equals(occasion.getId());
    }
  }

  @Override
  public int hashCode() {
    return this.toString().length();
  }
}
