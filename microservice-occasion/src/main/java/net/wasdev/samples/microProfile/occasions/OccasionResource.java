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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.ApplicationPath;
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

@RequestScoped
@ApplicationPath("/")
@Path("/")
public class OccasionResource {
  /** Access to MongoDB. */
  @Inject private MongoAccess mongo;

  private static final String clazz = OccasionResource.class.getName();
  private static final Logger logger = Logger.getLogger(clazz);
  public static final String JSON_KEY_OCCASION_POST_RUN_SUCCESS = "runSuccess";
  public static final String JSON_KEY_OCCASION_POST_RUN_ERROR = "runError";

  @Inject private Orchestrator orchestrator;

  /** The JWT of the caller. */
  @Inject private JsonWebToken jwtPrincipal;

  private DBCollection getCollection() {
    DB occasions = mongo.getMongoDB();
    return occasions.getCollection("occasions");
  }

  /** Read the occasions that are stored in the database and schedule them to run. */
  @PostConstruct
  public void afterCreate() {
    String method = "afterCreate";
    logger.entering(clazz, method);

    orchestrator.setOccasionResource(this);

    DBCollection occasions = getCollection();
    DBCursor cursor = occasions.find();
    while (cursor.hasNext()) {
      DBObject dbOccasion = cursor.next();
      Occasion occasion = new Occasion(dbOccasion);
      try {
        // TODO: There was a comment here about how we should mark the event as
        //       popped in the database, which will have different meaning for
        //       one-time or interval occasions.  Need to re-visit this.
        orchestrator.scheduleOccasion(occasion);
      } catch (Throwable t) {
        logger.log(Level.WARNING, "Could not schedule occasion at startup", t);
      }
    }

    logger.exiting(clazz, method);
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createOccasion(JsonObject json) {
    String method = "createOccasion";
    logger.entering(clazz, method, json);

    // Validate the JWT.  At this point, anyone create an occasion if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response;
    // make sure we received some json
    if (null == json || json.toString().isEmpty()) {
      response = Response.status(400).entity("Create failed. Empty payload.").build();
    } else {
      logger.fine("json = " + json);
      Occasion occasion = new Occasion(json);
      BasicDBObject dbo = occasion.toDbo();
      logger.fine("dbo = " + dbo);

      // new json payload should not contain an id
      ObjectId id = (ObjectId) dbo.get(Occasion.OCCASION_ID_KEY);
      logger.fine("id = " + id);
      if (null != id && !id.toString().isEmpty()) {
        logger.fine("non null id, responding 400");
        response =
            Response.status(400)
                .entity(
                    "Create failed. Payload must not contain an ID. Recieved ID: \"" + id + "\"")
                .build();
      } else {
        // store the occasion and return the ID
        getCollection().insert(dbo);
        ObjectId occasionId = dbo.getObjectId(Occasion.OCCASION_ID_KEY);
        logger.fine("id: " + occasionId.toString());
        String jsonResp =
            new Occasion(occasionId, null, null, null, null, null, null, null).toString();
        logger.fine(jsonResp);
        response = Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
        occasion.setId(occasionId);

        // Schedule occasion with the orchestrator
        try {
          orchestrator.scheduleOccasion(occasion);
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }
    }

    logger.exiting(clazz, method, response.readEntity(String.class));
    return response;
  }

  @POST
  @Path("/run")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response runOccasion(JsonObject json) {
    String method = "runOccasion";
    logger.entering(clazz, method, json);

    // Validate the JWT. At this point, anyone create an occasion if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response;
    // make sure we received some json
    if (null == json || json.toString().isEmpty()) {
      response = Response.status(400).entity("Run failed. Empty payload.").build();
    } else {
      logger.fine("json = " + json);
      Occasion occasion = new Occasion(json);
      BasicDBObject dbo = occasion.toDbo();
      logger.fine("dbo = " + dbo);

      // new json payload should not contain an id
      ObjectId id = (ObjectId) dbo.get(Occasion.OCCASION_ID_KEY);
      logger.fine("ObjectId id = " + id + " Occasion id = " + occasion.getId());

      if (null == id || id.toString().isEmpty()) {
        logger.fine("non null id, responding 400");
        response = Response.status(400).entity("Run failed. Payload must contain an ID.").build();
      } else {
        // Run the occasion
        OccasionResponse occasionResponse = orchestrator.runOccasion(occasion);
        response = buildPostRunResponse(occasionResponse);
      }
    }

    logger.exiting(clazz, method, response);
    return response;
  }

  Response buildPostRunResponse(OccasionResponse occasionResponse) {

    Throwable notificationThrowable = occasionResponse.getNotificationThrowable();
    String requestResponse = occasionResponse.getNotificationType();
    if (notificationThrowable != null) {
      logger.fine("Throwable message: " + notificationThrowable.getMessage());
    }
    JsonBuilderFactory factory = Json.createBuilderFactory(null);
    JsonObjectBuilder builder = factory.createObjectBuilder();
    JsonObject responseBody = null;
    if (requestResponse.equals(OccasionResponse.NOTIFICATION_TYPE_LOG)
        || requestResponse.equals(OccasionResponse.NOTIFICATION_TYPE_TWEET)) {
      responseBody = builder.add(JSON_KEY_OCCASION_POST_RUN_SUCCESS, requestResponse).build();
    } else {
      responseBody = builder.add(JSON_KEY_OCCASION_POST_RUN_ERROR, requestResponse).build();
    }
    return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response retrieveOccasion(@PathParam("id") String id) {
    String method = "retrieveOccasion";
    logger.entering(clazz, method, id);

    // Validate the JWT.  At this point, anyone can get an occasion if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response;
    // Ensure we recieved a valid ID
    if (!ObjectId.isValid(id)) {
      response = Response.status(400).entity("Invalid occasion id").build();
    } else {
      // perform the query and return the occasion, if we find one
      DBObject occasion =
          getCollection().findOne(new BasicDBObject(Occasion.OCCASION_ID_KEY, new ObjectId(id)));
      logger.fine("In " + method + " with Occasion: \n\n" + occasion);
      String occasionJson = new Occasion(occasion).toString();
      if (null == occasionJson || occasionJson.isEmpty()) {
        response = Response.status(400).entity("no occasion found for given id").build();
      } else {
        response = Response.ok(occasionJson, MediaType.APPLICATION_JSON).build();
      }
    }

    logger.exiting(clazz, method, response);
    return response;
  }

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response retrieveGroupsForOccasion(@QueryParam("groupId") String groupId) {
    String method = "retrieveOccasion";
    logger.entering(clazz, method);

    // Validate the JWT.  At this point, anyone can get the occasion list for a group
    // if they have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response;
    if (null == groupId || groupId.isEmpty()) {
      response = Response.status(400).entity("invalid group id").build();
    } else {
      // perform the query and return the list
      List<DBObject> occasionList =
          getCollection()
              .find(new BasicDBObject(Occasion.OCCASION_GROUP_ID_KEY, groupId))
              .toArray();
      JsonArray occasionListJson = Occasion.dboListToJsonArray(occasionList);
      response = Response.ok(occasionListJson, MediaType.APPLICATION_JSON).build();
    }

    logger.exiting(clazz, method, response);
    return response;
  }

  @PUT
  @Path("{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateOccasion(@PathParam("id") String id, JsonObject json) {
    String method = "updateOccasion";
    logger.entering(clazz, method, new Object[] {id, json});

    // Validate the JWT.  At this point, anyone can update an occasion
    // if they have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response = Response.ok().build();
    // Ensure we recieved a valid ID
    if (!ObjectId.isValid(id) || null == json || json.isEmpty()) {
      response = Response.status(400).entity("invalid occasion id").build();
    } else {
      // perform the update using $set to prevent overwriting data in the event of an incomplete
      // payload
      Occasion updatedOccasion = new Occasion(json);
      BasicDBObject updateObject = new BasicDBObject("$set", updatedOccasion.toDbo());
      BasicDBObject query = new BasicDBObject(Occasion.OCCASION_ID_KEY, new ObjectId(id));

      // Update and return the new document.
      DBObject updatedObject =
          getCollection().findAndModify(query, null, null, false, updateObject, true, false);

      // Reschedule occasion with the orchestrator.  Use the updated object from
      // mongo because it will contain all fields that the orchestator may need.
      try {
        orchestrator.cancelOccasion(id);
        orchestrator.scheduleOccasion(new Occasion(updatedObject));
      } catch (ParseException e) {
        e.printStackTrace();
        response =
            Response.status(400).entity("Invalid date given. Format must be YYYY-MM-DD").build();
      }
    }

    logger.exiting(clazz, method, response);
    return response;
  }

  @DELETE
  @Path("{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteOccasion(@PathParam("id") String id) {
    String method = "deleteOccasion";
    logger.entering(clazz, method, id);

    // Validate the JWT.  At this point, anyone can delete an occasion
    // if they have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      logger.exiting(clazz, method, Status.UNAUTHORIZED);
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    Response response;
    // Ensure we recieved a valid ID; empty query here deletes everything
    if (null == id || id.isEmpty() || !ObjectId.isValid(id)) {
      response = Response.status(400).entity("invalid occasion id").build();
    } else {
      // perform the deletion
      if (deleteOccasion(new ObjectId(id)) != true) {
        response = Response.status(400).entity("occasion deletion failed").build();
      } else {
        // Cancel occasion with the orchestrator
        orchestrator.cancelOccasion(id);

        response = Response.ok().build();
      }
    }

    logger.exiting(clazz, method, response);
    return response;
  }

  /**
   * Delete an occasion from the database
   *
   * @return true if we deleted one object, false if not (none, or more than one).
   */
  public boolean deleteOccasion(ObjectId idObject) {
    WriteResult result =
        getCollection().remove(new BasicDBObject(Occasion.OCCASION_ID_KEY, idObject));
    return (1 == result.getN());
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
