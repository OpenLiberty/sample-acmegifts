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
import { User } from '../user/user';
import { UserService } from '../user/services/user.service';
import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from '@angular/common/http';
import { ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-profile-edit',
  templateUrl: './profile-edit.component.html',
  providers: [UserService],
  styles: ['../../styles.css']
})
export class ProfileEditComponent implements OnInit {
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.'
    USER_UPDATE_RC_ERR_SERVER = 'User microservice server error during user profile update.';
    USER_DELETE_RC_ERR_SERVER = 'User microservice server error during user deletion.';
    USER_UPDATE_RC_ERR_USERNAME_EXITS = 'The username you specified is already taken.';
    USER_RC_ERR_UNAUTH = 'The request was not authorized. Please be sure you are logged in before you continue.';
    
    userId: string;
    usernameAlreadyExists = false;
    eventMessage: string = null;
    user = new User('', '', '', '', '', '', '', '');
    cachedUser = new User('', '', '', '', '', '', '', '');
    returnUrl: string;
    
    constructor(private route: ActivatedRoute,
                private http: HttpClient,
                private userService: UserService,
                private router: Router,
                private location: Location) { }

    ngOnInit() {
        this.returnUrl = this.route.snapshot.queryParams['returnUrl'];
        
        this.route.paramMap.subscribe(params => {
            this.userId = params.get('id');

            // If we did not receive a user id, the JWT token is, most likely, expired.
            if (this.userId === '' || this.userId === undefined || this.userId === null) {
                this.eventMessage = this.MSG_USER_ID_INVALID;
                return;
            }

            // Get the current user's data.
            this.userService.getUser(this.userId).subscribe(resp => { 
                    this.user = resp; 
                    this.cachedUser = this.user;
                    sessionStorage.userName = this.user.userName;
                }, err => {
                    // Report the error and stay on the same page.
                    sessionStorage.userName = '';
                    this.eventMessage = this.MSG_USER_NOT_RETRIEVED;
                });
        });
    }

    onUpdateUser(): void {
        this.userService.updateUser(this.user)
            .subscribe((httpResp: HttpResponse<any>) => {
                this.cachedUser = this.user
                sessionStorage.userName = this.user.userName;
                sessionStorage.userId = this.user.id;
                this.location.back();
            }, (err: any) => {
                if (err.status === 400) {
                    this.eventMessage = this.USER_UPDATE_RC_ERR_USERNAME_EXITS;
                } else if (err.status === 401) {
                    this.eventMessage = this.USER_RC_ERR_UNAUTH;
                } else {
                    this.eventMessage = this.USER_UPDATE_RC_ERR_SERVER;
                }
            });
    }

    onCancel() {
        // Go back to the previous page.
        this.user = this.cachedUser;
        this.location.back();
    }
    
    onDeleteProfile() {
        this.userService.deleteUser(this.userId)
        .subscribe((httpResp: HttpResponse<any>) => {
            this.router.navigate(['logout']);
        }, (err: any) => {
            if (err.status === 401) {
                this.eventMessage = this.USER_RC_ERR_UNAUTH;
            } else { 
                this.eventMessage = this.USER_DELETE_RC_ERR_SERVER;
            }
        });
    }

    onCloseEventBox() {
        this.eventMessage = null;
    }
}