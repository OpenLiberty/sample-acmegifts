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

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Microservice for user management. */
@Path("/users")
@RequestScoped
public class UserResource {

  /** Access to MongoDB where we store user data. */
  @Inject private MongoAccess mongo;

  /**
   * The JWT of the current caller. Since this is a request scoped resource, the JWT will be
   * injected for each JAX-RS request. The injection is performed by the mpJwt-1.0 feature.
   */
  @Inject private JsonWebToken jwtPrincipal;

  /**
   * Adds a new user.
   *
   * @param payload A JSON object containing the attributes for the new user.
   * @return The ID of the new user.
   */
  @POST
  @Path("/")
  @Consumes("application/json")
  public Response addUser(JsonObject payload) {

    // Validate the JWT. The caller must have a JWT in the "login" group to
    // create a new user.
    try {
      validateJWT(new HashSet<String>(Arrays.asList("login")));
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Make sure the user provided a password.
    String userPassword = payload.getString("password", "");
    if ((userPassword == null) || (userPassword.trim().length() == 0)) {
      return Response.status(Status.BAD_REQUEST)
          .entity("The request must contain a password")
          .build();
    }

    // Hash the password for storage in the database.  Prepend a random salt to make
    // the resulting hash less likely to be useful for someone trying to use the
    // password on another site.
    PasswordUtility pwUtil = null;
    try {
      pwUtil = new PasswordUtility(userPassword);
    } catch (Throwable t) {
      return Response.serverError().entity("Could not hash password").build();
    }

    JsonObjectBuilder builder =
        createJsonBuilder(payload)
            .add(User.JSON_KEY_USER_PASSWORD_HASH, pwUtil.getHashedPassword())
            .add(User.JSON_KEY_USER_PASSWORD_SALT, pwUtil.getSalt());

    // This will create a "non-Twitter" user from the modified JSON input.
    User user = new User(builder.build());

    // Build the JWT that the caller should use on all future calls.
    // The builder ID is specified in server.xml. We build this first
    // because we don't want to add the user if we can't build the response.
    String jwtTokenString = null;
    try {
      jwtTokenString =
          JwtBuilder.create("jwtUserBuilder")
              .claim(Claims.SUBJECT, user.getUserName())
              .claim("upn", user.getUserName()) /* MP-JWT defined subject claim */
              .claim(
                  "groups",
                  "users") /* MP-JWT defined group, seems Liberty makes an array from a comma separated list */
              .buildJwt()
              .compact();
    } catch (Throwable t) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Erorr building authorization token")
          .build();
    }

    // Go ahead and add the user to the database.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);

    if (dbCollection.findOne(new BasicDBObject(User.JSON_KEY_USER_NAME, user.getUserName()))
        != null) {
      return Response.status(Status.BAD_REQUEST).entity("The user already exists.").build();
    }

    BasicDBObject dbEntry = user.getDBObject(false);
    dbCollection.insert(dbEntry);
    String dbId = dbEntry.getString(User.DB_ID);

    // The response will contain the ID of the newly created user.
    String responsePayload = (new BasicDBObject(User.JSON_KEY_USER_ID, dbId)).toString();

    return Response.ok(responsePayload)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenString)
        .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION)
        .build();
  }

  /**
   * Update an existing user.
   *
   * @param id The ID of the user to update.
   * @param payload The fields of the user that should be updated.
   * @return Nothing.
   */
  @PUT
  @Path("/{id}")
  @Consumes("application/json")
  public Response updateUser(@PathParam("id") String id, JsonObject payload) {

    // Validate the JWT. The JWT should be in the 'users' group. We do not
    // check to see if the user is modifying their own profile.
    try {
      validateJWT(new HashSet<String>(Arrays.asList("users")));
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Retrieve the user from the database.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
    DBObject oldDbUser = dbCollection.findOne(new ObjectId(id));

    if (oldDbUser == null) {
      return Response.status(Status.BAD_REQUEST).entity("The user was not Found.").build();
    }

    // If the input object contains a new password, need to hash it for use in the database.
    User newUser = null;
    if (payload.containsKey("password")) {
      try {
        String rawPassword = payload.getString("password");
        String saltString = (String) (oldDbUser.get(User.JSON_KEY_USER_PASSWORD_SALT));
        PasswordUtility pwUtil = new PasswordUtility(rawPassword, saltString);
        JsonObject newJson =
            createJsonBuilder(payload)
                .add(User.JSON_KEY_USER_PASSWORD_HASH, pwUtil.getHashedPassword())
                .add(User.JSON_KEY_USER_PASSWORD_SALT, pwUtil.getSalt())
                .build();
        newUser = new User(newJson);
      } catch (Throwable t) {
        return Response.serverError().entity("Error updating password").build();
      }
    } else {
      newUser = new User(payload);
    }

    // Create the updated user object.  Only apply the fields that we want the
    // client to change (skip the internal fields).
    DBObject updateObject = new BasicDBObject("$set", newUser.getDBObjectForModify());
    dbCollection.findAndModify(oldDbUser, updateObject);

    return Response.ok().build();
  }

  /**
   * Delete a user.
   *
   * @param id The ID of the user to delete.
   * @return Nothing.
   */
  @DELETE
  @Path("/{id}")
  public Response deleteUser(@PathParam("id") String id) {
    // Validate the JWT.  The JWT must be in the 'users' group.  We do not check
    // to see if the user is deleting their own profile.
    try {
      validateJWT(new HashSet<String>(Arrays.asList("users")));
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Retrieve the user from the database.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
    ObjectId dbId = new ObjectId(id);
    DBObject dbUser = dbCollection.findOne(dbId);

    // If the user did not exist, return an error.  Otherwise, remove the user.
    if (dbUser == null) {
      return Response.status(Status.BAD_REQUEST).entity("The user name was not Found.").build();
    }

    dbCollection.remove(new BasicDBObject(User.DB_ID, dbId));
    return Response.ok().build();
  }

  /**
   * Retrieve a user's profile.
   *
   * @param id The ID of the user.
   * @return The user's profile, as a JSON object. Private fields such as password and salt are not
   *     returned.
   */
  @GET
  @Path("/{id}")
  @Produces("application/json")
  public Response getUser(@PathParam("id") String id) {
    // Validate the JWT.  The JWT must belong to the 'users' or 'orchestrator' group.
    // We do not check if the user is retrieving their own profile, or someone else's.
    try {
      validateJWT(new HashSet<String>(Arrays.asList("users", "orchestrator")));
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Retrieve the user from the database.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
    DBObject user = dbCollection.findOne(new ObjectId(id));

    // If the user did not exist, return an error.  Otherwise, only return the public
    // fields (exclude things like the password).
    if (user == null) {
      return Response.status(Status.BAD_REQUEST).entity("The user not Found.").build();
    }

    JsonObject responsePayload = new User(user).getPublicJsonObject();

    return Response.ok(responsePayload, MediaType.APPLICATION_JSON).build();
  }

  /**
   * Get all user profiles.
   *
   * @return All user profiles (excluding private fields like password).
   */
  @GET
  @Produces("application/json")
  public Response getAllUsers() {
    // Validate the JWT. The JWT must be in the 'users' group.
    try {
      validateJWT(new HashSet<String>(Arrays.asList("users")));
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Get all the users from the database, and add them to an array.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
    DBCursor cursor = dbCollection.find();
    JsonArrayBuilder userArray = Json.createArrayBuilder();
    while (cursor.hasNext()) {
      // Exclude all private information from the list.
      userArray.add((new User(cursor.next()).getPublicJsonObject()));
    }

    // Return the user list to the caller.
    JsonObjectBuilder responseBuilder = Json.createObjectBuilder().add("users", userArray.build());
    return Response.ok(responseBuilder.build(), MediaType.APPLICATION_JSON).build();
  }

  /** Allow for a JsonObject to be modified */
  public JsonObjectBuilder createJsonBuilder(JsonObject source) {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (String key : source.keySet()) {
      builder.add(key, source.get(key));
    }
    return builder;
  }

  /** Do some basic checks on the JWT, until the MP-JWT annotations are ready. */
  private void validateJWT(Set<String> validGroups) throws JWTException {
    // Make sure the authorization header was present. This check is somewhat
    // silly since the jwtPrincipal will never actually be null since it's a
    // WELD proxy (injected).
    if (jwtPrincipal == null) {
      throw new JWTException("No authorization header or unable to inflate JWT");
    }

    // Make sure we're in one of the groups that is authorized.
    String validatedGroupName = null;
    Set<String> groups = jwtPrincipal.getGroups();
    if (groups != null) {
      for (String group : groups) {
        if (validGroups.contains(group)) {
          validatedGroupName = group;
          break;
        }
      }
    }

    if (validatedGroupName == null) {
      throw new JWTException("User is not in a valid group [" + groups.toString() + "]");
    }
  }

  private static class JWTException extends Exception {
    private static final long serialVersionUID = 423763L;

    public JWTException(String message) {
      super(message);
    }
  }
}
