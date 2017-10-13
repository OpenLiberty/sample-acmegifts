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
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../user/services/user.service';
import { LoginService } from './services/login.service';

class TwitterOauthData {
  constructor(
      public oauthToken: string,
      public oauthVerifier: string
  ) {  }

}

@Component({
  template: '',
  providers: [UserService, LoginService]
})
export class TwitterVerifyComponent implements OnInit {

    private sub: any = null;

    constructor(private http: HttpClient, private route: ActivatedRoute,
                private router: Router, private userService: UserService,
                private loginService: LoginService) {}

    // This performs the second part of the twitter login.  We're
    // basically delegating to the user microservice, who should
    // verify the tokens we've received from Twitter are valid, and
    // build a JWT for us to use on future requests.
    ngOnInit() {
        const oauthData = new TwitterOauthData('', '');
        this.sub = this.route.queryParams.subscribe(params => {
            oauthData.oauthToken = params['oauth_token'];
            oauthData.oauthVerifier = params['oauth_verifier'];
        });

        // Maven fills in these variables from the pom.xml
        const url = 'https://${user.hostname}:${user.https.port}/logins/twitter/verify';
        const body = JSON.stringify(oauthData);
        let headers = new HttpHeaders();
        headers = headers.set('Content-Type', 'application/json');
        headers = headers.set('Authorization', sessionStorage.jwt);

        this.loginService.loginWithTwitterVerify(body).subscribe((res: HttpResponse<any>) => {
            const id: string = res.body['id'];
            const twitterLogin: boolean = res.body['twitter'];

            if (twitterLogin === true) {
                // Cache the JWT for use on future calls.
                sessionStorage.jwt = res.headers.get('Authorization');

                // Get the username and cache it.
                this.userService.getUser(id).subscribe(resp => {
                    sessionStorage.userName = resp.userName;
                    sessionStorage.userId = id;

                    // Change to the groups view
                    this.routeToGroups(id);
                }, err => {
                    // Route to the login page again with error message
                    delete sessionStorage.jwt;
                    this.router.navigate([ '/login' ], { queryParams: {'retryMessage': 'serverError'} });
                });
            } else {
                // Route to login page again with error message
                this.router.navigate([ '/login' ], { queryParams: {'retryMessage': 'retryUserPw'} });
            }
        }, (err: any) => {
            this.router.navigate([ '/login' ], { queryParams: {'retryMessage': 'serverError'} });
        });
    }

    routeToGroups(id: string): void {
        this.router.navigate(['/groups', {userId: id}]);
    }
}
