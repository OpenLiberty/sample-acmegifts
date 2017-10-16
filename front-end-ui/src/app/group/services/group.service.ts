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
import 'rxjs/add/operator/map';
import { Group } from '../group';
import { Groups } from '../groups';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';

/**
 * Perform operations with the group backend microservice.
 */
@Injectable()
export class GroupService {
    private groups: Group[];

    // Maven fills in these variables from the pom.xml
    private url = '${group.service.url}';

  constructor(private http: HttpClient) {
      this.url = this.url + ((this.url.indexOf('/', this.url.length - 1) === -1) ? '/': '') ;

      if (sessionStorage.jwt == null) {
          console.log('JSON Web Token is not available. Login before you continue.');
      }
  }

  getGroups(userId: string): Observable<Groups> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.get<Groups>(this.url + '?userId=' + userId, { headers: headers })
      .map(data => data)
  }

  getGroup(groupId: string): Observable<Group> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.get<Group>(this.url + groupId, { headers: headers })
      .map(data => data)
  }

  createGroup(payload: string): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'application/json');
      headers = headers.set('Authorization', sessionStorage.jwt);
      
      return this.http.post<HttpResponse<any>>(this.url, payload, { headers: headers })
      .map(data => data);
  }

  updateGroup(groupId: string, payload: string): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'application/json');
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.put<HttpResponse<any>>(this.url + groupId, payload, { headers: headers })
      .map(data => data);
  }

  deleteGroup(groupId: string): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.delete<HttpResponse<any>>(this.url + groupId, { headers: headers })
      .map(data => data);
  }
}
