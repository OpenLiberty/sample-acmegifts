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
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  template: ''
})
export class LogoutComponent implements OnInit {

    constructor(private router: Router) {}

    ngOnInit() {
        // Remove the JWT from web storage to logout the user.
        delete sessionStorage.jwt;
        delete sessionStorage.userName;
        delete sessionStorage.userId;
        
        // Route back to the initial page.
        this.router.navigate(['login']);
    }
}