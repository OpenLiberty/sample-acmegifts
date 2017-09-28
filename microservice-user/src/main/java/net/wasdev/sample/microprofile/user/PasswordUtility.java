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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * This utility class generates password salt values, and hashes the password for storing in the
 * database.
 */
public class PasswordUtility {
  /** Source of entropy for things that need random numbers. */
  private static final SecureRandom saltGenerator = new SecureRandom();

  /**
   * Salt value prepended to password bytes to make it harder for someone to look up passwords
   * stored in the database using a 'rainbow table'. Each password gets a unique salt value.
   */
  private final byte[] salt;

  /** The hashed password that is stored in the database. */
  private final String hashedPassword;

  /**
   * Take a cleartext password and generate a salt and the hashed password that we'll put in the
   * database.
   *
   * @throws NoSuchAlgorithmException Thrown if the SHA-256 algorithm is not supported.
   */
  PasswordUtility(String userPassword) throws NoSuchAlgorithmException {
    // Hash the password for storage in the database.  Prepend a random salt to make
    // the resulting hash less likely to be useful for someone trying to use the
    // password on another site.
    salt = new byte[16];
    saltGenerator.nextBytes(salt);

    // Get a new message digest used to hash the password, and prepend it with the
    // salt.
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(salt);

    // Add the password to the digest and then Base64 encode the result.
    hashedPassword = Base64.getEncoder().encodeToString(md.digest(userPassword.getBytes()));
  }

  /**
   * Create a new hashed password from an existing salt.
   *
   * @throws NoSuchAlgorithmException
   * @throws Base64Exception
   */
  PasswordUtility(String userPassword, String saltString) throws NoSuchAlgorithmException {
    salt = Base64.getDecoder().decode(saltString);

    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(salt);
    hashedPassword = Base64.getEncoder().encodeToString(md.digest(userPassword.getBytes()));
  }

  /** Get the salt as a string, for storage in a document. */
  public String getSalt() {
    return Base64.getEncoder().encodeToString(salt);
  }

  /** Get the hashed password as a string, for storage in a document. */
  public String getHashedPassword() {
    return hashedPassword;
  }
}
