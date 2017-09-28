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
package net.wasdev.samples.microprofile.group;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.bson.types.ObjectId;

/** This class represents a group object. */
public class Group {

  public static final String DB_COLLECTION_NAME = "groups";
  public static final String DB_ID = "_id";

  public static final String JSON_KEY_GROUP_ID = "id";
  public static final String JSON_KEY_GROUP_NAME = "name";
  public static final String JSON_KEY_MEMBERS_LIST = "members";
  public static final String JSON_KEY_GROUPS = "groups";

  private String id;
  private String name;
  private String[] members;

  /**
   * Create a group based on a Mongo DB Object
   *
   * @param group The existing Mongo DB Object
   */
  public Group(DBObject group) {
    this.id = ((ObjectId) group.get(DB_ID)).toString();
    this.name = (String) group.get(JSON_KEY_GROUP_NAME);

    BasicDBList dbMembers = ((BasicDBList) group.get(JSON_KEY_MEMBERS_LIST));
    this.members = new String[dbMembers.size()];
    for (int i = 0; i < dbMembers.size(); i++) {
      members[i] = (String) dbMembers.get(i);
    }
  }

  /**
   * Create a group based on a JSON Object
   *
   * @param group The JSON Object with group information
   */
  public Group(JsonObject group) {
    if (group.containsKey(JSON_KEY_GROUP_ID)) {
      id = group.getString(JSON_KEY_GROUP_ID);
    }
    name = group.getString(JSON_KEY_GROUP_NAME);

    JsonArray jsonMembers = group.getJsonArray(JSON_KEY_MEMBERS_LIST);
    if (jsonMembers != null) {
      members = new String[jsonMembers.size()];
      for (int i = 0; i < jsonMembers.size(); i++) {
        members[i] = jsonMembers.getString(i);
      }
    } else {
      members = new String[0];
    }
  }

  /**
   * Create a JSON string based on the content of this group
   *
   * @return - A JSON string with the content of this group
   */
  public String getJson() {
    JsonObjectBuilder group = Json.createObjectBuilder();
    group.add(JSON_KEY_GROUP_ID, id);
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
}
