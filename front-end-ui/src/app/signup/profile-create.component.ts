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
import { AuthService } from '../auth/services/auth.service';
import { UserService } from '../user/services/user.service';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  templateUrl: './profile-create.component.html',
  providers: [AuthService, UserService],
  styles: ['../../styles.css']
})
export class ProfileCreateComponent implements OnInit {
    USER_CREATE_RC_ERR_FIREFOX_SEC_MSG = 'NOTE: You are using Firefox. Special security certificate processing is needed. ' +
     'See the documentation for more details.';
    JWT_LOGIN_GET_RC_ERR = 'The auth service returned an error. Please be sure the auth server is started.';
    USER_RC_ERR_UNAUTH = 'The request was not authorized. Please be sure you are logged in before you continue.';
    USER_CREATE_RC_ERR_USERNAME_EXITS = 'The username you specified is already taken.';
    USER_CREATE_RC_ERR_SERVER = 'User microservice server error during user creation.';

    id: any;
    badUsername: string;
    firefoxWarning = false;
    firefoxMessage: string = null;
    eventMessage: string = null;
    user = new User('', '', '', '', '', '', '', '');
    
    constructor(private http: HttpClient,
                private userService: UserService,
                private authService: AuthService,
                private router: Router) { }

    ngOnInit() {
        this.firefoxWarning = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
    }

    onCreateUser(): void {
        this.authService.getLoginJwt().subscribe((res: HttpResponse<any>) => {
            sessionStorage.jwt = res.headers.get('Authorization');

            this.userService.createUser(this.user)
                .subscribe((res2: HttpResponse<any>) => {
                    sessionStorage.jwt = res2.headers.get('Authorization');
                    this.id = res2.body['id'];
                    sessionStorage.userName = this.user.userName;
                    sessionStorage.userId = this.id;
                    this.routeToGroups(this.id);
                }, (err: any) => {
                    if (err.status === 400) {
                        this.eventMessage = this.USER_CREATE_RC_ERR_USERNAME_EXITS;
                    } else if (err.status === 401) {
                        this.eventMessage = this.USER_RC_ERR_UNAUTH;
                    } else {
                        this.eventMessage = this.USER_CREATE_RC_ERR_SERVER;
                        if (this.firefoxWarning) {
                            this.firefoxMessage = this.USER_CREATE_RC_ERR_FIREFOX_SEC_MSG;
                        }
                    }
            });
        }, (err: any) => {
            this.eventMessage = this.JWT_LOGIN_GET_RC_ERR;
            if (this.firefoxWarning) {
                this.firefoxMessage = this.USER_CREATE_RC_ERR_FIREFOX_SEC_MSG;
            }
        });
    }

    onCancel() {
        this.user = new User('', '', '', '', '', '', '', '');
        this.router.navigate(['/login']);
    }
    
    onCloseEventBox() {
        this.eventMessage = null;
    }
    
    routeToGroups(id: string): void {
        this.router.navigate(['/groups', {userId: id}]);
    }
}
