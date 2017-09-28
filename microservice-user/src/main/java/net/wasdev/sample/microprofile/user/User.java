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
package net.wasdev.sample.microprofile.user;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.bson.types.ObjectId;

/** An Object representation of the user data stored in MongoDB. */
public class User {
  /** The collection name used in MongoDB. */
  public static final String DB_COLLECTION_NAME = "users";

  /** The field name of the unique ID in MongoDB. */
  public static final String DB_ID = "_id";

  /* These are the keys used to store the user data in MongoDB */
  public static final String JSON_KEY_USER_ID = "id";
  public static final String JSON_KEY_USER_FIRST_NAME = "firstName";
  public static final String JSON_KEY_USER_LAST_NAME = "lastName";
  public static final String JSON_KEY_USER_NAME = "userName";
  public static final String JSON_KEY_USER_TWITTER_HANDLE = "twitterHandle";
  public static final String JSON_KEY_USER_WISH_LIST_LINK = "wishListLink";
  public static final String JSON_KEY_USER_TWITTER_LOGIN = "isTwitterLogin";
  public static final String JSON_KEY_USER_PASSWORD_HASH = "password";
  public static final String JSON_KEY_USER_PASSWORD_SALT = "salt";

  /** The unique ID for the user. */
  private String id;

  /** The user's first name. */
  private String firstName;

  /** The user's last name. */
  private String lastName;

  /** The name that the user will use to log into the application. */
  private String userName;

  /** The user's twitter username. */
  private String twitterHandle;

  /** A URL pointing to the user's wish list. */
  private String wishListLink;

  /** The hashed password that is stored in the database for this user. */
  private String passwordHash;

  /** The generated salt that is contained in the hashed password. */
  private String passwordSalt;

  /** Was this user created here, or as a result of a twitter login? */
  private boolean isTwitterLogin;

  /** Constructor for reading a user from the database */
  public User(DBObject user) {
    this.id = ((ObjectId) user.get(DB_ID)).toString();
    this.firstName = (String) user.get(JSON_KEY_USER_FIRST_NAME);
    this.lastName = (String) user.get(JSON_KEY_USER_LAST_NAME);
    this.userName = (String) user.get(JSON_KEY_USER_NAME);
    this.twitterHandle = (String) user.get(JSON_KEY_USER_TWITTER_HANDLE);
    this.wishListLink = (String) user.get(JSON_KEY_USER_WISH_LIST_LINK);
    this.passwordHash = (String) user.get(JSON_KEY_USER_PASSWORD_HASH);
    this.passwordSalt = (String) user.get(JSON_KEY_USER_PASSWORD_SALT);
    this.isTwitterLogin = (Boolean) user.get(JSON_KEY_USER_TWITTER_LOGIN);
  }

  /** Constructor for reading the user from the JSON that was a part of a JAX-RS request. */
  public User(JsonObject user) {
    if (user.containsKey(JSON_KEY_USER_ID)) {
      id = user.getString(JSON_KEY_USER_ID);
    }
    this.firstName = user.getString(JSON_KEY_USER_FIRST_NAME, "");
    this.lastName = user.getString(JSON_KEY_USER_LAST_NAME, "");
    this.twitterHandle = user.getString(JSON_KEY_USER_TWITTER_HANDLE, "");
    this.wishListLink = user.getString(JSON_KEY_USER_WISH_LIST_LINK, "");
    this.isTwitterLogin = user.getBoolean(JSON_KEY_USER_TWITTER_LOGIN, false);

    if (user.containsKey(JSON_KEY_USER_NAME)) {
      this.userName = user.getString(JSON_KEY_USER_NAME);
    }
    if (user.containsKey(JSON_KEY_USER_PASSWORD_HASH)) {
      this.passwordHash = user.getString(JSON_KEY_USER_PASSWORD_HASH);
    }
    if (user.containsKey(JSON_KEY_USER_PASSWORD_SALT)) {
      this.passwordSalt = user.getString(JSON_KEY_USER_PASSWORD_SALT);
    }
  }

  /**
   * Create a User object from a Twitter login
   *
   * @param name The user name as reported by Twitter. We will try to parse this into a first and
   *     last name. This value may be null.
   * @param twitterHandle The twitter user's name. This will become the user name for the user.
   * @return A partial User object. The user will need to edit their profile to complete it.
   */
  public User(String name, String twitterHandle) {
    // Try to parse the name into a first and last name.
    if (name != null) {
      String aName = name.trim();
      if (aName.length() > 0) {
        int indexOfLastSpace = aName.lastIndexOf(' ');
        if (indexOfLastSpace != -1) {
          // Separate first/last name on the last space.
          this.firstName = aName.substring(0, indexOfLastSpace);
          this.lastName = aName.substring(indexOfLastSpace + 1);
        } else {
          // Couldn't find a space - just use this as last name.
          this.lastName = aName;
          this.firstName = "";
        }
      } else {
        this.lastName = "";
        this.firstName = "";
      }
    } else {
      this.lastName = "";
      this.firstName = "";
    }

    // Use the twitter handle as the user name and the twitter handle.
    this.userName = twitterHandle;
    this.twitterHandle = twitterHandle;

    // We don't store any password information for twitter users. The
    // wish list link will need to be filled in manually using the
    // user editor in the application.
    this.passwordHash = "";
    this.passwordSalt = "";
    this.wishListLink = "";
    this.isTwitterLogin = true;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getUserName() {
    return userName;
  }

  public String getTwitterHandle() {
    return twitterHandle;
  }

  public String getWishListLink() {
    return wishListLink;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getPasswordSalt() {
    return passwordSalt;
  }

  public boolean isTwitterLogin() {
    return isTwitterLogin;
  }

  /**
   * Return a JSON object suitable to be returned to the caller with no confidential information
   * (like password or salt).
   */
  public JsonObject getPublicJsonObject() {
    JsonObjectBuilder user = Json.createObjectBuilder();
    user.add(JSON_KEY_USER_ID, id);
    user.add(JSON_KEY_USER_FIRST_NAME, firstName);
    user.add(JSON_KEY_USER_LAST_NAME, lastName);
    user.add(JSON_KEY_USER_NAME, userName);
    user.add(JSON_KEY_USER_TWITTER_HANDLE, twitterHandle);
    user.add(JSON_KEY_USER_WISH_LIST_LINK, wishListLink);
    user.add(JSON_KEY_USER_TWITTER_LOGIN, isTwitterLogin);

    return user.build();
  }

  /** Return an object suitable to create a new user in MongoDB. */
  public BasicDBObject getDBObject(boolean includeId) {
    BasicDBObject user = new BasicDBObject();
    if (includeId) {
      user.append(DB_ID, new ObjectId(id));
    }
    user.append(JSON_KEY_USER_FIRST_NAME, firstName);
    user.append(JSON_KEY_USER_LAST_NAME, lastName);
    user.append(JSON_KEY_USER_NAME, userName);
    user.append(JSON_KEY_USER_TWITTER_HANDLE, twitterHandle);
    user.append(JSON_KEY_USER_WISH_LIST_LINK, wishListLink);
    user.append(JSON_KEY_USER_PASSWORD_HASH, passwordHash);
    user.append(JSON_KEY_USER_PASSWORD_SALT, passwordSalt);
    user.append(JSON_KEY_USER_TWITTER_LOGIN, isTwitterLogin);

    return user;
  }

  /**
   * Return a DBObject that can be used for updating the user in the mongo database. We only include
   * the fields that the client should be changing (leave out the internal fields).
   */
  public DBObject getDBObjectForModify() {
    BasicDBObject user = new BasicDBObject();
    user.append(JSON_KEY_USER_FIRST_NAME, firstName);
    user.append(JSON_KEY_USER_LAST_NAME, lastName);
    user.append(JSON_KEY_USER_WISH_LIST_LINK, wishListLink);

    // If the user logs in with Twitter, don't let them change their
    // twitter handle, username, or password.
    if (isTwitterLogin == false) {
      user.append(JSON_KEY_USER_TWITTER_HANDLE, twitterHandle);
      if (userName != null) {
        user.append(JSON_KEY_USER_NAME, userName);
      }
      if (passwordHash != null) {
        user.append(JSON_KEY_USER_PASSWORD_HASH, passwordHash);
      }
      if (passwordSalt != null) {
        user.append(JSON_KEY_USER_PASSWORD_SALT, passwordSalt);
      }
    }
    return user;
  }
}
