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
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';

// This component is responsible for putting the header at the top
// of the page if the user has logged in.
@Component({
    selector: 'app-header',
    templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit {

    userName: string = null;
    userId: string = null;

    constructor(private router: Router,
                iconRegistry: MatIconRegistry,
                sanitizer: DomSanitizer) {
        // Need to register the icon that we'll use to display the menu
        // in the header.  This is required by the material code that
        // we use to display the menu (icon).
        iconRegistry.addSvgIcon(
            'hamburger',
            sanitizer.bypassSecurityTrustResourceUrl('assets/images/icon_menu.svg'));
    }

    ngOnInit() {
        this.userName = sessionStorage.userName;
        this.userId = sessionStorage.userId;

        // Every time the route changes (user navigates to a new
        // page/view), check to see if the user name is still set
        // in session storage.  If there is a change, the HTML
        // for the header will be hidden or shown.
        this.router.events.subscribe((event) => {
            this.userName = sessionStorage.userName;
            this.userId = sessionStorage.userId;
        });
    }

    onLogout(): void {
        this.router.navigate(['logout']);
    }
    
    onEditProfile(): void {
        this.router.navigate(['profiles',sessionStorage.userId, 'edit']);
    }
}
