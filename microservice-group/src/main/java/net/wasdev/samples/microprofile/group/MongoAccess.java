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

import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Holds the MongoDB instance that the JAX-RS resources will use. */
@ApplicationScoped
public class MongoAccess {
  /** The mongoDB hostname */
  @Inject
  @ConfigProperty(name = "mongo.hostname")
  private String mongoHostname;

  /** The mongoDB port */
  @Inject
  @ConfigProperty(name = "mongo.port")
  private int mongoPort;

  /** Cached DB reference used by all threads. */
  private DB database = null;

  /** Get a connection to Mongo */
  public synchronized DB getMongoDB() {
    if (database == null) {
      try {
        MongoClient client = new MongoClient(mongoHostname, mongoPort);
        database = client.getDB("gifts-group");
      } catch (UnknownHostException uhe) {
        throw new RuntimeException(uhe);
      }
    }

    return database;
  }
}
