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
import { Login } from '../loginObject';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth/services/auth.service';
import { LoginService } from './services/login.service';
import { UserService } from '../user/services/user.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  providers: [AuthService, LoginService, UserService]
})
export class LoginComponent implements OnInit {

    LOGIN_RC_ERR_USR_NOT_RETRIEVED = 'The user information could not be retrieved.';
    LOGIN_RC_ERR_USR_NOT_FOUND = 'userNotFound';
    LOGIN_RC_ERR_USRNAME_PWD_INVALID_GEN_MSG = 'The username and/or password you entered did not match our records. Please try again.';
    LOGIN_RC_ERR_INCORRECT_PSWD = 'incorrectPassword';
    LOGIN_RC_ERR_CANNOT_AUTH =  'unableToAuthenticate';
    LOGIN_RC_ERR_CANNOT_AUTH_MSG = 'The server was unable to authenticate the user';
    LOGIN_RC_ERR_CLIENT_NETWORK_MSG = 'Network connectivity or client error';
    LOGIN_RC_ERR_FIREFOX_SEC_MSG = 'NOTE: You are using Firefox. Special security certificate processing is needed. ' +
     'See the documentation for more details.';

    LOGIN_TWTR_RC_ERR_RETRY_PARM = 'retryMessage';

    LOGIN_TWTR_RC_ERR_RETRY_UNAME_PSWD = 'retryUserPw';
    LOGIN_TWTR_RC_ERR_RETRY_UNAME_PSWD_MSG = 'Login with a username and password';
    LOGIN_TWTR_RC_ERR_NO_CONFIG = 'noTwitterConfig';
    LOGIN_TWTR_RC_ERR_NO_CONFIG_MSG = 'Twitter configuration (pom.xml) is not present.';
    LOGIN_TWTR_RC_ERR_SRVR_ERROR = 'serverError';
    LOGIN_TWTR_RC_ERR_SRVR_ERROR_MSG = 'A server error (HTTP 500) occurred.';

    login = new Login('', '');
    private sub: any = null;
    firefoxWarning = false;
    firefoxMessage: string = null;
    eventMessage: string = null;

    constructor(private http: HttpClient, private router: Router,
                private route: ActivatedRoute,
                private loginService: LoginService,
                private authService: AuthService,
                private userService: UserService) {}

    // Check to see if we were routed here with an error message.
    ngOnInit() {
        this.firefoxWarning = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
        this.firefoxMessage = null;

        this.sub = this.route.queryParams.subscribe(params => {
            if ('retryMessage' in params) {
                const retryCode: string = params[this.LOGIN_TWTR_RC_ERR_RETRY_PARM];
                if (retryCode === this.LOGIN_TWTR_RC_ERR_RETRY_UNAME_PSWD) {
                    this.eventMessage = this.LOGIN_TWTR_RC_ERR_RETRY_UNAME_PSWD_MSG;
                } else if (retryCode === this.LOGIN_TWTR_RC_ERR_NO_CONFIG) {
                    this.eventMessage = this.LOGIN_TWTR_RC_ERR_NO_CONFIG_MSG;
                } else if (retryCode === this.LOGIN_TWTR_RC_ERR_SRVR_ERROR) {
                    this.eventMessage = this.LOGIN_TWTR_RC_ERR_SRVR_ERROR_MSG;
                    if (this.firefoxWarning) {
                        this.firefoxMessage = this.LOGIN_RC_ERR_FIREFOX_SEC_MSG;
                    }
                } else {
                    this.eventMessage = 'Error message \'' + retryCode + '\' is not defined.';
                }
            }
        });
    }

    onLoginUser(): void {
        this.firefoxMessage = null;
        const body = JSON.stringify(this.login);

        this.authService.getLoginJwt().subscribe((res2: HttpResponse<any>) => {
            sessionStorage.jwt = res2.headers.get('Authorization');

            this.loginService.login(body).subscribe((res: HttpResponse<any>) => {
                const id: string = res.body['id'];
                const twitterLogin: boolean = res.body['twitter'];

                if (twitterLogin === false) {
                    // Cache the JWT for use on future calls.
                    sessionStorage.jwt = res.headers.get('Authorization');

                    // Get the username and cache it.
                    this.userService.getUser(id).subscribe(resp => {
                        sessionStorage.userName = resp.userName;
                        sessionStorage.userId = id;

                        // Change to the groups view.
                        this.routeToGroups(id);
                    }, err => {
                        // Stay here and report an error.
                        delete sessionStorage.jwt;
                        this.eventMessage = this.LOGIN_RC_ERR_USR_NOT_RETRIEVED;
                    });
                } else {
                    // Stay here and ask user to try again.
                    this.eventMessage = 'You must login with Twitter';
                }
            }, (err: HttpErrorResponse) => {
                if (err.error instanceof Error) {
                    // Client error or network error.
                    this.eventMessage = this.LOGIN_RC_ERR_CLIENT_NETWORK_MSG;
                    console.log('A client or network error occurred:', err.message);
                } else {
                    // Backend error
                    let error: string;
                    if (err.error !== null) {
                        error = err.error['error'];
                    }

                    if (error === this.LOGIN_RC_ERR_USR_NOT_FOUND) {
                        this.eventMessage = this.LOGIN_RC_ERR_USRNAME_PWD_INVALID_GEN_MSG;
                    } else if (error === this.LOGIN_RC_ERR_INCORRECT_PSWD) {
                        this.eventMessage = this.LOGIN_RC_ERR_USRNAME_PWD_INVALID_GEN_MSG;
                    } else if (error === this.LOGIN_RC_ERR_CANNOT_AUTH) {
                        this.eventMessage = this.LOGIN_RC_ERR_CANNOT_AUTH_MSG;
                    } else {
                        this.eventMessage = `Server error (HTTP ${err.status}) has occurred.`;
                        if (this.firefoxWarning) {
                            this.firefoxMessage = this.LOGIN_RC_ERR_FIREFOX_SEC_MSG;
                        }
                    }

                    console.log(`Login. The server response status is: ${err.status}. Error message: ` + err.message);
                }
            });
        }, (err: HttpErrorResponse) => {
            this.eventMessage = `Auth server error (HTTP ${err.status}) has occurred.`;
        });
    }

    onTwitterSignIn() {
        this.router.navigate(['/login/twitter']);
    }

    onCloseEventBox() {
        this.eventMessage = null;
    }

    routeToGroups(id: string): void {
        this.router.navigate(['/groups', {userId: id}]);
    }
}
