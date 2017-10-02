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
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { User } from '../user';
import { Users } from '../users';

/**
 * Perform operations with the user backend microservice.
 */
@Injectable()
export class UserService {
    private users: User[];

    private userUrl = '/service/users/';
    private userUrl2 = '/service/users';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<Users> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.get<Users>(this.userUrl2, { headers: headers })
      .map(data => data);
  }

  getUser(userId: string): Observable<User> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.get<User>(this.userUrl + userId, { headers: headers })
      .map(data => data);
  }

  createUser(user: User): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'application/json');
      headers = headers.set('Authorization', sessionStorage.jwt);

      const payload = JSON.stringify(user);
      return this.http.post<HttpResponse<any>>(this.userUrl, payload, { headers: headers, observe: 'response' })
      .map(data => data);
  }

  updateUser(user: User): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'application/json');
      headers = headers.set('Authorization', sessionStorage.jwt);
      const payload = JSON.stringify(user);

      return this.http.put<HttpResponse<any>>(this.userUrl + user.id, payload, { headers: headers, observe: 'response' })
      .map(data => data);
  }
  
  deleteUser(userId: string): Observable<HttpResponse<any>> {
      let headers = new HttpHeaders();
      headers = headers.set('Authorization', sessionStorage.jwt);

      return this.http.delete<HttpResponse<any>>(this.userUrl + userId, { headers: headers })
      .map(data => data);
  }
}
