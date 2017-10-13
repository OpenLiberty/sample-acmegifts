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
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';

@Injectable()
export class LoginService {
  
    // Maven fills in these variables from the pom.xml
    private url = '${user.service.login.url}';

    constructor(private http: HttpClient) {
        this.url = this.url + ((this.url.indexOf('/', this.url.length - 1) === -1) ? '/': '') ;
    }

    login(payload: string): Observable<HttpResponse<any>> {
        let headers = new HttpHeaders();
        headers = headers.set('Content-Type', 'application/json');
        headers = headers.set('Authorization', sessionStorage.jwt);

        return this.http.post<HttpResponse<any>>(this.url, payload, { headers: headers, observe: 'response'}).map(data => data);
    }

    loginWithTwitter(): Observable<HttpResponse<any>> {
        let headers = new HttpHeaders();
        headers = headers.set('Authorization', sessionStorage.jwt);

        return this.http.get(this.url + 'twitter', {headers: headers, observe: 'response'}).map(data => data);
    }

    loginWithTwitterVerify(payload: string): Observable<HttpResponse<any>> {
        let headers = new HttpHeaders();
        headers = headers.set('Content-Type', 'application/json');
        headers = headers.set('Authorization', sessionStorage.jwt);

        return this.http.post<HttpResponse<any>>(
                this.url + 'twitter/verify', payload, { headers: headers, observe: 'response'}).map(data => data);
    }
}
