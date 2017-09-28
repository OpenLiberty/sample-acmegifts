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
package net.wasdev.sample.microprofile.notification;

import java.io.File;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Notification resource. It allows callers to log notifications. */
@Path("/notifications")
@RequestScoped
public class NotificationResource {

  private static final String JSON_KEY_NOTIFICATION = "notification";

  Logger logger =
      Logger.getLogger("net.wasdev.sample.microprofile.notification.NotificationResource");

  @Inject
  @ConfigProperty(name = "log.file")
  private String logFile;

  /** The JWT of the caller. */
  @Inject private JsonWebToken jwtPrincipal;

  @PostConstruct
  public void init() {
    try {
      File log = new File(logFile);
      File parentDir = log.getParentFile();
      if (!parentDir.exists()) {
        parentDir.mkdirs();
      }

      FileHandler fh = new FileHandler(logFile);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/")
  @Consumes("application/json")
  public Response notify(JsonObject payload) {

    // Validate the JWT.  At this point, anyone can submit a notification if they
    // have a valid JWT.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    String notification = payload.getString(JSON_KEY_NOTIFICATION);
    logger.info(notification);

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

    // At this point, only the orchestrator can make notifications.
    Set<String> groups = jwtPrincipal.getGroups();
    if (groups.contains("orchestrator") == false) {
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
