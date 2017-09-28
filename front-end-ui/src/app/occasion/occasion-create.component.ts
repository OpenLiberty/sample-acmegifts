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
import 'rxjs/add/operator/switchMap';

import { ActivatedRoute, ParamMap, Router} from '@angular/router';
import { Component, OnDestroy, OnInit, OnChanges } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';

import { Group } from '../group/group';
import { GroupService } from '../group/services/group.service';
import { Observable } from 'rxjs/Rx';
import { Occasion } from './Occasion';
import { OccasionService } from './services/occasion.service';
import { Contribution } from './Contribution';
import { User } from '../user/user';
import { UserService } from '../user/services/user.service';

@Component({
    selector: 'app-occasion',
    templateUrl: './occasion-create.component.html',
    providers: [GroupService, UserService],
    styles: ['../../styles.css', '../../app.css']
})
export class OccasionCreationComponent implements OnInit {
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.';
    MSG_ONE_OR_MORE_USERS_NOT_RETRIEVED = 'The information of one or more users could not be retrieved.';
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_GRP_ID_INVALID = 'The group you attempted to view is not known. Please, refresh your list of groups and try again.';

    contributionAmount : number;
    groupMembers: User[] = [];
    group        = new Group('', '', []);
    occasion             = new Occasion('', [], '', '', '', '', '');
    user                 = new User('', '', '', '', '', '', '', '');
    eventMessage: string = null;

    constructor(private occasionService: OccasionService,
        private userService: UserService,
        private groupService: GroupService,
        private route: ActivatedRoute,
        private router: Router,
        private http: HttpClient) {}

    ngOnInit() {
        this.route.paramMap.subscribe(params => {
            const userId = params.get('userId');

            // If we did not receive a user id, the JWT token is, most likely, expired.
            if (userId === '' || userId === undefined || userId === null) {
                this.eventMessage = this.MSG_USER_ID_INVALID;
                return;
            }

            const groupId = params.get('groupId');

            // If we did not receive a group id, the group has been, most likely, deleted.
            if (groupId === '' || groupId === undefined || groupId === null) {
                this.eventMessage = this.MSG_GRP_ID_INVALID;
                return;
            }

            // Get the current user's data.
            this.occasion.organizerId = userId;
            this.userService.getUser(this.occasion.organizerId).subscribe((user: User) => { 
                    this.user = user;
                    sessionStorage.userName = this.user.userName;
                }, err => {
                    // Report the error and stay on the same page.
                    sessionStorage.userName = '';
                    this.eventMessage = this.MSG_USER_NOT_RETRIEVED;
                });

            // Get current group's data.
            this.occasion.groupId = groupId;
            this.groupService.getGroup(this.occasion.groupId).subscribe((group: Group) => {
                this.group = group;
                this.populateMemberList();
            }, err => {
                this.eventMessage = 'An error occurred obtaining the group from the server.';
            });
        });
    }

    onCreateOccasion() {
        if (null != (this.eventMessage = this.validateDate())) {
            return;
        }

        const contribution: Contribution = new Contribution(this.occasion.organizerId, Number(this.contributionAmount));
        this.occasion.contributions = [];
        this.occasion.contributions.push(contribution);

        this.occasionService.createOccasion(this.occasion)
            .subscribe((resp: JSON) => {
                this.occasion._id = resp['_id'];
                this.routeToGroupView();
            });
    }

    onCancel() {
        this.routeToGroupView();
    }

    onCloseEventBox() {
        this.eventMessage = null;
    }

    routeToGroupView(): void {
        this.router.navigate([ '/groups/group', { userId : this.user.id, groupId: this.group.id} ]);
    }

    populateMemberList() {
        const groupMembers: User[] = [];
        let errorGettingUserDetected = false;
        for (const memberId of this.group.members) {
            this.userService.getUser(memberId).subscribe((member: User) => {
                groupMembers.push(member);
            }, err => {
                errorGettingUserDetected = true;
            });
        }
        
        // If there were some errors getting user information, report the 
        // error and stay on the same page.
        if (errorGettingUserDetected) {
            this.eventMessage = this.MSG_ONE_OR_MORE_USERS_NOT_RETRIEVED;
        }
        
        this.setGroupMembers(groupMembers);
    }

    setGroupMembers(groupMembers: User[]) {
        this.groupMembers = groupMembers;
    }

    validateDate(): string {

        const dateArr   = this.occasion.date.split('-');
        const year      = parseInt(dateArr[0], 10);
        const month     = parseInt(dateArr[1], 10);
        const day       = parseInt(dateArr[2], 10);

        const today     = new Date();
        const currDay   = today.getDay();
        const currMonth = today.getMonth();
        const currYear  = today.getFullYear();
        
        const errMsg = 'Invalid date given.';
        if (year < currYear) {
            return errMsg;
        } else if ((month > 12)
            || ((year === currYear)
                && (month < currMonth))) {
            return errMsg;
        } else if ((!this.isValidDay(year, month, day))
            || ((year === currYear)
                && (month === currMonth)
                && (day < currDay))) {
            return errMsg;
        }

        return null;
    }

    isValidDay(year: number, month: number, day: number): boolean {
        
        const daysInaMonth = [ 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ];

        /* Handle leap years */
        if (year % 4 === 0) {
            if (year % 100 === 0) {
                if (year % 400 === 0) {
                    daysInaMonth [1] = 29;
                } 
            } else {
                daysInaMonth [1] = 29;
            }
        }
        return (day > 0) && (day <= daysInaMonth[month - 1]);
    }
}
