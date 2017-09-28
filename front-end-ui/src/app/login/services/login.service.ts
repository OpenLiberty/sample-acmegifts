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
    private url = 'https://${user.hostname}:${user.https.port}/logins/';

  constructor(private http: HttpClient) { }

  login(payload: string): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'application/json');
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.post<HttpResponse<any>>(this.url, payload, { headers: headers, observe: 'response'}).map(data => data);
  }
}
