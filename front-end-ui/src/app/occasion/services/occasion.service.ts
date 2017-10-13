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
import { ActivatedRoute, Router } from '@angular/router';
import { Group } from '../../group/group';
import { GroupService } from '../../group/services/group.service';
import { Groups } from '../../group/groups';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { Occasion } from '../Occasion';
import { Contribution } from '../Contribution';
import { User } from '../../user/user';
import { UserService } from '../../user/services/user.service';

@Injectable()
export class OccasionService {
    private jwt: string = null;

    // Maven fills in these variables from the pom.xml
    private groupUrl = '${group.service.url}';
    private userUrl = '${user.service.url}';   
    private occasionUrl = '${occasion.service.url}';

    constructor(private route: ActivatedRoute,
        private groupService: GroupService,
        private http: HttpClient,
        private router: Router,
        private userService: UserService) {

        this.groupUrl = this.groupUrl + ((this.groupUrl.indexOf('/', this.groupUrl.length - 1) === -1) ? '/': '') ;
        this.userUrl = this.userUrl + ((this.userUrl.indexOf('/', this.userUrl.length - 1) === -1) ? '/': '');
        this.occasionUrl = this.occasionUrl + ((this.occasionUrl.indexOf('/', this.occasionUrl.length - 1) === -1) ? '/': '');

        if (sessionStorage.jwt === null ||
            sessionStorage.jwt === 'null' ||
            sessionStorage.jwt === '"null"' ||
            sessionStorage.jwt === '') {
            console.log('Json Web Token is not available. Login before you continue.');
        }
    }

    createOccasion(occasion: Occasion): Observable<JSON> {
        const payload = JSON.stringify(occasion);
        let headers   = new HttpHeaders();
        headers       = headers.set('Content-Type', 'application/json');
        headers       = headers.set('Authorization', sessionStorage.jwt);

        // If we have a JWT, then go ahead.  Otherwise, route
        // back to the main page.
        if (sessionStorage.jwt === null ||
            sessionStorage.jwt === 'null' ||
            sessionStorage.jwt === '"null"' ||
            sessionStorage.jwt === '') {
            this.router.navigate([ '/login' ]);
        }

        return this.http.post<JSON>(this.occasionUrl, payload, { headers: headers })
               .map(data => data)
               .catch(error => Observable.throw('Occasion Microservice HTTP POST Occasion Server Error'));
    }

    updateOccasion(occasion: Occasion): Observable<JSON> {
        const payload = JSON.stringify(occasion);
        let headers   = new HttpHeaders();
        headers       = headers.set('Content-Type', 'application/json');
        headers       = headers.set('Authorization', sessionStorage.jwt);

        if (sessionStorage.jwt === null ||
            sessionStorage.jwt === 'null' ||
            sessionStorage.jwt === '"null"' ||
            sessionStorage.jwt === '') {
            this.router.navigate([ '/login' ]);
        }

        return this.http.put<JSON>(this.occasionUrl + occasion._id, payload, { headers: headers })
        .map(data => data)
        .catch(error => Observable.throw('Occasion Microservice HTTP PUT Occasion Server Error'));
    }

    runOccasion(occasion: Occasion): Observable<HttpResponse<any>> {
        const payload = JSON.stringify(occasion);
        let headers   = new HttpHeaders();
        headers       = headers.set('Content-Type', 'application/json');
        headers       = headers.set('Authorization', sessionStorage.jwt);

        // If we have a JWT, then go ahead.  Otherwise, route
        // back to the main page.
        if (sessionStorage.jwt === null || 
            sessionStorage.jwt === 'null' || 
            sessionStorage.jwt === '"null"' || 
            sessionStorage.jwt === '') {
            this.router.navigate([ '/login' ]);
        }

        return this.http.post<JSON>(this.occasionUrl + 'run/', payload, { headers: headers, observe: 'response' })
               .map(data => data)
               .catch(error => Observable.throw('Occasion Microservice HTTP POST/run Occasion Server Error'));
    }

    getOccasion(occasionId: string): Observable<Occasion> {
        if (sessionStorage.jwt === null ||
            sessionStorage.jwt === 'null' ||
            sessionStorage.jwt === '"null"' ||
            sessionStorage.jwt === '') {
            this.router.navigate([ '/login' ]);
        }

        let headers = new HttpHeaders();
        headers = headers.set('Authorization', sessionStorage.jwt);

        return this.http.get<Occasion>(this.occasionUrl + occasionId, { headers: headers })
               .map(data => data)
               .catch(error => Observable.throw('Occasion Microservice HTTP GET Occasion Server Error'));
    }

    getOccasionsForGroup(groupId: string): Observable<Occasion[]> {
        let headers = new HttpHeaders();
        headers     = headers.set('Authorization', sessionStorage.jwt);

        return this.http.get<Occasion[]>(this.occasionUrl + '?groupId=' + groupId, { headers: headers })
        .map(data => data)
        .catch(error => Observable.throw('Occasion Microservice HTTP GET Occasions Server Error'));

    }

    deleteOccasion(occasionId: string): Observable<string> {
        let headers = new HttpHeaders();
        headers = headers.set('Authorization', sessionStorage.jwt);

        return this.http.delete<string>(this.occasionUrl + occasionId, { headers: headers })
        .map(data => data)
        .catch(error => Observable.throw('Occasion Microservice HTTP DELETE Occasions Server Error'));
    }

    destroy(destroyMe: any) {
        destroyMe.sub.unsubscribe();
    }
}
