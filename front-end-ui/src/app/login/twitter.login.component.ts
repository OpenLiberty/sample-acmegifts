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
import { HttpClient, HttpHeaders, HttpErrorResponse, HttpResponse} from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../auth/services/auth.service';

@Component({
  template: '',
  providers: [AuthService]
})
export class TwitterLoginComponent implements OnInit {

    constructor(private http: HttpClient, private router: Router,
                private authService: AuthService ) {}

    // This performs the first part of the twitter login.  We're
    // basically delegating to the user microservice, who should
    // respond with an oauth token which we'll use to build a URL
    // and redirect to Twitter where the user will enter their Twitter
    // credentials.
    ngOnInit() {
        this.authService.getLoginJwt().subscribe((res2: HttpResponse<any>) => {
            sessionStorage.jwt = res2.headers.get('Authorization');

            let headers = new HttpHeaders();
            headers = headers.set('Authorization', sessionStorage.jwt);

            // Maven fills in these variables from the pom.xml
            const url = 'https://${user.hostname}:${user.https.port}/logins/twitter';
            this.http.get(url, {headers: headers, observe: 'body'}).subscribe(resBody => {
            if ('error' in resBody) {
                this.router.navigate([ '/login' ], { queryParams: {'retryMessage': resBody['error']} });
            } else {
                const oauthToken: string = resBody['oauth_token'];
                window.location.href = 'https://api.twitter.com/oauth/authenticate?oauth_token=' + oauthToken;
            }
            }, (err: any) => {
                this.router.navigate([ '/login' ], { queryParams: {'retryMessage': 'serverError'} });
            });
        }, (err: any) => {
            this.router.navigate([ '/login' ], { queryParams: {'retryMessage': 'serverError'} });
        });
    }
}
