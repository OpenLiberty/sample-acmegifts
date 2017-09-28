/*******************************************************************************
* Copyright (c) 2017 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* IBM Corporation - initial API and implementation
*******************************************************************************/
export class User {
  constructor(
    public id: string,
    public firstName: string,
    public lastName: string,
    public userName: string,
    public wishListLink: string,
    public twitterHandle: string,
    public password: string,
    public isTwitterLogin: string
  ) {}
}
