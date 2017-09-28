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
package net.wasdev.sample.microprofile.auth;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtBuilder;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The auth resource is responsible for providing a JWT to the caller, which can be used to login
 * with the user service, or create a user with the user service. The JWT is a member of the "login"
 * group and therefore can only access resources which this group is permitted to.
 *
 * <p>In the future, the auth resource should be modified to contain all JWT creation. Currently,
 * the user service will issue a new JWT once the user has logged in, and the occasion service will
 * issue its own JWT when an occasion is ready to run.
 */
@Path("/auth")
@RequestScoped
public class AuthResource {

  /**
   * Make a JWT that can be used only to login or create a user. The JWT will expire in a short
   * amount of time, and the group should only allow a login.
   */
  @GET
  public Response generateJwtLogin() {
    // Build a JWT that the caller can use to create a new user or login.
    // The builder ID is specified in server.xml.  We build this first
    // because we don't want to add the user if we can't build the response.
    String jwtTokenString = null;
    try {
      jwtTokenString =
          JwtBuilder.create("jwtAuthLoginBuilder")
              .claim(Claims.SUBJECT, "unauthenticated")
              .claim("upn", "unauthenticated") /* MP-JWT defined subject claim */
              .claim(
                  "groups",
                  "login") /* MP-JWT defined group, seems Liberty makes an array from a comma separated list */
              .buildJwt()
              .compact();
    } catch (Throwable t) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Erorr building authorization token")
          .build();
    }

    return Response.ok()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenString)
        .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION)
        .build();
  }
}
