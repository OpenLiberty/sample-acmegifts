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

import com.ibm.websphere.security.jwt.Claims;
import javax.enterprise.context.ApplicationScoped;

/** Implementation of the JWT Builder for Liberty. */
@ApplicationScoped
public class JwtBuilderImpl implements JwtBuilder {

  @Override
  public String buildCompactJWT(String groupName, String userName) {
    try {
      return com.ibm.websphere.security.jwt.JwtBuilder.create("jwtOccasionBuilder")
          .claim(Claims.SUBJECT, userName)
          .claim("upn", userName)
          .claim("groups", groupName)
          .buildJwt()
          .compact();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
