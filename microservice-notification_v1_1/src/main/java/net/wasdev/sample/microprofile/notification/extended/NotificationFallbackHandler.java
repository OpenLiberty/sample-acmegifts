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

import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

@Dependent
public class NotificationFallbackHandler implements FallbackHandler<String> {

  @Override
  public String handle(ExecutionContext context) {
    Object[] tweetParameters = context.getParameters();
    String message = (String) tweetParameters[0];
    Logger fbLogger = (Logger) tweetParameters[2];
    fbLogger.info(message);
    return null;
  }
}
