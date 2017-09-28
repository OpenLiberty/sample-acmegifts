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
export class Group {
    public id: string;
    public name: string;
    public members: string[];

    constructor( id: string, name: string, members: string[]) {
        this.id = id;
        this.name = name;
        this.members = members;
    }
}
