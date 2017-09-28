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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import org.bson.types.ObjectId;

/** This class represents a group object for testing purposes */
public class Group {
  public static final String DB_COLLECTION_NAME = "groups";
  public static final String DB_ID = "_id";

  public static final String JSON_KEY_GROUP_ID = "id";
  public static final String JSON_KEY_GROUP_NAME = "name";
  public static final String JSON_KEY_MEMBERS_LIST = "members";

  public static final String JSON_KEY_GROUPS_FOR_USER_LIST = "groups";

  private String id;
  private String name;
  private String[] members;

  public Group(String json) throws Exception {
    JsonObject jObject = stringToJsonObj(json);
    this.id = jObject.getString(JSON_KEY_GROUP_ID);
    this.name = jObject.getString(JSON_KEY_GROUP_NAME);

    JsonArray jsonMembers = jObject.getJsonArray(JSON_KEY_MEMBERS_LIST);
    if (jsonMembers != null) {
      members = new String[jsonMembers.size()];
      for (int i = 0; i < jsonMembers.size(); i++) {
        members[i] = jsonMembers.getString(i);
      }
    } else {
      members = new String[0];
    }
  }

  public Group(String id, String name, String[] members) {
    this.id = id;
    this.name = name;
    this.members = members;
  }

  /** Sets the Id value. */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Create a JSON string based on the content of this group
   *
   * @return The JSON string with the content of this group
   */
  public String getJson() {
    JsonObjectBuilder group = Json.createObjectBuilder();
    if (id != null) {
      group.add(JSON_KEY_GROUP_ID, id);
    }

    group.add(JSON_KEY_GROUP_NAME, name);

    JsonArrayBuilder membersArray = Json.createArrayBuilder();
    for (int i = 0; i < members.length; i++) {
      membersArray.add(members[i]);
    }
    group.add(JSON_KEY_MEMBERS_LIST, membersArray.build());

    return group.build().toString();
  }

  /**
   * Create a Mongo DB Object baed on the content of this group
   *
   * @param id The Mongo Object id to assign to this DB Object. If null, a new Object id will be
   *     created
   * @return - The Mongo DB Object based on the content of this group
   */
  public BasicDBObject getDBObject(boolean includeId) {
    BasicDBObject group = new BasicDBObject();

    if (includeId) {
      group.append(DB_ID, new ObjectId(id));
    }

    group.append(JSON_KEY_GROUP_NAME, name);

    BasicDBList membersArray = new BasicDBList();
    for (int i = 0; i < members.length; i++) {
      membersArray.add(members[i]);
    }
    group.append(JSON_KEY_MEMBERS_LIST, membersArray);

    return group;
  }

  public boolean isEqual(BasicDBObject other) {
    BasicDBList oMembers = (BasicDBList) other.get(JSON_KEY_MEMBERS_LIST);

    return ((oMembers.containsAll(Arrays.asList(this.members))
            && oMembers.size() == this.members.length)
        && this.name.equals(other.get(JSON_KEY_GROUP_NAME))
        && this.id.equals(other.getString(DB_ID)));
  }

  public boolean isEqual(Group other) {
    List<String> oMembers = Arrays.asList(other.members);

    return ((oMembers.containsAll(Arrays.asList(this.members))
            && oMembers.size() == this.members.length)
        && this.name.equals(other.name)
        && this.id.equals(other.id));
  }

  public static JsonObject stringToJsonObj(String input) {
    try {
      JsonReader jsonReader = Json.createReader(new StringReader(input));
      JsonObject output = jsonReader.readObject();
      jsonReader.close();
      return output;
    } catch (JsonParsingException e) {
      return null;
    }
  }
}
