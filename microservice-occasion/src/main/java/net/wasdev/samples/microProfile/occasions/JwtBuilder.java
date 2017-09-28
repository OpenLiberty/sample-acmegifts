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

/**
 * Liberty provides a JWT builder with static methods that can be used to build a JWT. We are
 * abstracting that out here, so that we can run unit tests without the Liberty runtime in place.
 */
public interface JwtBuilder {
  /**
   * Build a JWT in compact form (base 64 encoded and signed).
   *
   * @param groupName The group name that should be in the JWT claims.
   * @param userName The user name that should be in the JWT claims.
   * @return A MP-compliant JWT.
   */
  public String buildCompactJWT(String groupName, String userName);
}
