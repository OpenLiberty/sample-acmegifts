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
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("groups")
@RequestScoped
public class GroupResource {
  /** Access to Mongo DB */
  @Inject private MongoAccess mongo;

  public DBCollection getGroupCollection() {
    DB groupsDB = mongo.getMongoDB();
    return groupsDB.getCollection(Group.DB_COLLECTION_NAME);
  }

  /** The JWT of the caller. */
  @Inject private JsonWebToken jwtPrincipal;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createGroup(JsonObject payload) {

    // Validate the JWT. At this point, anyone can create a group if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Create a new group based on the payload information
    Group newGroup = new Group(payload);

    // Create a db object from the group content and insert it into the
    // collection
    BasicDBObject dbGroup = newGroup.getDBObject(false);
    getGroupCollection().insert(dbGroup);

    // Return the new group id
    JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
    String groupId = dbGroup.getString(Group.DB_ID);
    responseBuilder.add(Group.JSON_KEY_GROUP_ID, groupId);

    JsonObject response = responseBuilder.build();
    return Response.ok(response, MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGroupInfo(@PathParam("id") String id) {

    // Validate the JWT. At this point, anyone can get a group info if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Check if the id is a valid mongo object id. If not, the server will
    // throw a 500
    if (!ObjectId.isValid(id)) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group was not found")
          .build();
    }

    // Query Mongo for group with specified id
    BasicDBObject group = (BasicDBObject) getGroupCollection().findOne(new ObjectId(id));

    if (group == null) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group was not found")
          .build();
    }

    // Create a JSON payload with the group content
    String responsePayload = (new Group(group)).getJson();

    return Response.ok().entity(responsePayload).build();
  }

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGroups(@QueryParam("userId") String userId) {
    // Validate the JWT. At this point, anyone can get a group list if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    DBCursor groupCursor = null;
    BasicDBList groupList = new BasicDBList();
    if (userId != null) {
      if (!ObjectId.isValid(userId)) {
        return Response.status(Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN)
            .entity("The user id provided is not valid.")
            .build();
      }

      BasicDBObject queryObj = new BasicDBObject(Group.JSON_KEY_MEMBERS_LIST, userId);
      groupCursor = getGroupCollection().find(queryObj);
    } else {
      groupCursor = getGroupCollection().find();
    }

    while (groupCursor.hasNext()) {
      groupList.add((new Group(groupCursor.next()).getJson()));
    }

    String responsePayload = (new BasicDBObject(Group.JSON_KEY_GROUPS, groupList)).toString();

    return Response.ok(responsePayload).build();
  }

  @DELETE
  @Path("{id}")
  public Response deleteGroup(@PathParam("id") String id) {

    // Validate the JWT. At this point, anyone can delete a group if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Check if the id is a valid mongo object id. If not, the server will
    // throw a 500
    if (!ObjectId.isValid(id)) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group ID is not valid.")
          .build();
    }

    // Query Mongo for group with specified id
    BasicDBObject group = (BasicDBObject) getGroupCollection().findOne(new ObjectId(id));

    if (group == null) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group was not found")
          .build();
    }

    // Delete group
    getGroupCollection().remove(group);

    return Response.ok().build();
  }

  @PUT
  @Path("{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateGroup(@PathParam("id") String id, JsonObject payload) {

    // Validate the JWT. At this point, anyone can update a group if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Check if the id is a valid mongo object id. If not, the server will
    // throw a 500
    if (!ObjectId.isValid(id)) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group was not found")
          .build();
    }

    // Query Mongo for group with specified id
    BasicDBObject oldGroup = (BasicDBObject) getGroupCollection().findOne(new ObjectId(id));

    if (oldGroup == null) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .entity("The group was not found")
          .build();
    }

    // Create new group based on payload content
    Group updatedGroup = new Group(payload);

    // Update database with new group
    getGroupCollection().findAndModify(oldGroup, updatedGroup.getDBObject(true));

    return Response.ok().build();
  }

  /** Do some basic checks on the JWT, until the MP-JWT annotations are ready. */
  private void validateJWT() throws JWTException {
    // Make sure the authorization header was present.  This check is somewhat
    // silly since the jwtPrincipal will never actually be null since it's a
    // WELD proxy (injected).
    if (jwtPrincipal == null) {
      throw new JWTException("No authorization header or unable to inflate JWT");
    }

    // Make sure we're in one of the groups we know about.
    Set<String> groups = jwtPrincipal.getGroups();
    if ((groups.contains("users") == false) && (groups.contains("orchestrator") == false)) {
      throw new JWTException("User is not in a valid group [" + groups.toString() + "]");
    }

    // TODO: Additional checks as appropriate.
  }

  private static class JWTException extends Exception {
    private static final long serialVersionUID = 423763L;

    public JWTException(String message) {
      super(message);
    }
  }
}
