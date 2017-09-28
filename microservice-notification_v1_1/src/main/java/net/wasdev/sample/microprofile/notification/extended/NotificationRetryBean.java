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
package net.wasdev.sample.microprofile.notification.extended;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import twitter4j.Twitter;

@ApplicationScoped
public class NotificationRetryBean {

  /**
   * Retry twice in the event of failure, after which the fallback handler will be driven to write
   * the message to a fallback log.
   */
  @Retry(maxRetries = 2)
  @Fallback(NotificationFallbackHandler.class)
  public String tweet(
      String message, Twitter twitter, Logger fbLogger, String handle, Logger logger)
      throws Exception {
    try {
      // Tweet the occasion (AcmeGifts Twitter account). If the message is
      // longer that 140 chars, the message is split.
      // For example: abcdef: "abc ..." "... def".
      List<String> msgList = preProcessMessage(message);
      for (String msg : msgList) {
        twitter.updateStatus(msg);
      }

      // Send a direct message to the occasion recipient.
      twitter.sendDirectMessage(handle, message);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Tweet error", e);
      throw e;
    }

    return null;
  }

  /**
   * Returns a list of strings that are less than or equal to 140 chars in length.
   * Partial/continuing strings are suffixed/prefixed with "... " respectively.
   *
   * @param message The message to process.
   * @return The list processed strings.
   */
  public List<String> preProcessMessage(String message) {
    List<String> messages = new ArrayList<String>();
    boolean split = true;
    while (split) {
      if (message.length() <= 140) {
        messages.add(message);
        split = false;
      }
      if (message.length() > 140) {
        int i = message.lastIndexOf(" ", 132);
        String partial = message.substring(0, i) + " ...";
        messages.add(partial);
        message = "... " + message.substring(i + 1, message.length());
      }
    }

    return messages;
  }
}
