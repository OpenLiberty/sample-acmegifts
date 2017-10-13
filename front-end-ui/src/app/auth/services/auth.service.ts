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
    private url: string = "${auth.service.url}";

    constructor(private http: HttpClient) {
        this.url = this.url + ((this.url.indexOf('/', this.url.length - 1) === -1) ? '/': '') ;
    }

    getLoginJwt(): Observable<HttpResponse<any>> {
        return this.http.get<HttpResponse<any>>(this.url, { observe: 'response'}).map(data => data);
    }
}
