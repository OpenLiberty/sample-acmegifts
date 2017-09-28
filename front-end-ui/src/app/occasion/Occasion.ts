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
import { Contribution } from './Contribution';
export class Occasion {
    constructor(
        public _id: string,
        public contributions: Contribution[],
        public date: string,
        public groupId: string,
        public name: string,
        public organizerId: string,
        public recipientId: string
    ) {}
}
