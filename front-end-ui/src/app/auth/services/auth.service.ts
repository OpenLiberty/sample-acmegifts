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

/**
 * Perform operations with the authorization backend microservice.
 */
@Injectable()
export class AuthService {
    // Maven fills in these variables from the pom.xml
    private url = 'https://${auth.hostname}:${auth.https.port}/auth/';

    constructor(private http: HttpClient) {}

    getLoginJwt(): Observable<HttpResponse<any>> {
        return this.http.get<HttpResponse<any>>(this.url, { observe: 'response'}).map(data => data);
    }
}
