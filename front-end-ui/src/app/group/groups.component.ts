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
import { Group } from './group';
import { GroupContribution } from './group-contribution';
import { GroupService } from './services/group.service';
import { Groups } from './groups';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { User } from '../user/user';
import { UserService } from '../user/services/user.service';
import { Occasion } from '../occasion/Occasion';
import { OccasionService } from '../occasion/services/occasion.service';
import { HttpErrorResponse} from '@angular/common/http';

@Component({
    selector: 'app-groups',
    templateUrl: './groups.component.html',
    providers: [GroupService, UserService, OccasionService]
})
export class GroupsComponent implements OnInit {
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.';
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_RC_ERR_CLIENT_NETWORK = 'Network connectivity or client error';
    
    GROUP_ERROR_ID_NOT_VALID = 'The group ID is not valid';
    GROUP_ERROR_ID_NOT_FOUND = 'The group was not found';
    GROUP_ERROR_ID_NOT_VALID_MSG = 'The group service returned an error indicating that the group ID was not valid.';
    GROUP_ERROR_ID_NOT_FOUND_MSG = 'The group service returned an error indicating that the group was not found.';

    groups: Group[];
    userId = '';
    user: User = new User('', '', '', '', '', '', '', '');
    loggedInUserTotalContribution = 0;
    loggedInUserGroupContributionList: GroupContribution[] = [];
    eventMessageError: string = null;

    constructor(private route: ActivatedRoute,
        private groupService: GroupService,
        private userService: UserService,
        private occasionService: OccasionService,
        private router: Router) {}

    ngOnInit() {
        this.route.paramMap.subscribe(params => {
            this.userId = params.get('userId');

            // If we did not receive a user id, the JWT token is, most likely, expired.
            if (this.userId === '' || this.userId === undefined || this.userId === null) {
                this.eventMessageError = this.MSG_USER_ID_INVALID;
                return;
            }
            
            // Get the current user's data.
            this.userService.getUser(this.userId).subscribe(resp => { 
                this.user = resp;
                sessionStorage.userName = this.user.userName;
            }, err => {
                // Report the error and stay on the same page.
                sessionStorage.userName = '';
                this.eventMessageError = this.MSG_USER_NOT_RETRIEVED;
            });

            // Get the current user's groups.
            this.getGroupsForUser();
        });
    }

    onGetGroupName(obj): string {
        return this.getJObj(obj).name;
    }

    onDisplayGroup(group: Group) {
        const jGroup = this.getJObj(group);
        this.router.navigate(['/groups/group', {groupId : jGroup.id, userId: this.userId}]);
    }

    onCreateGroup() {
        this.router.navigate(['/groups/group/create', {userId : this.userId}]);
    }

    onDelete(group: Group) {
        const jGroup = this.getJObj(group);
        this.groupService.deleteGroup(jGroup.id).subscribe(resp => {
            this.getGroupsForUser();
        }, (err: HttpErrorResponse) => {
            if (err.error instanceof Error) {
                // Client error or network error.
                this.eventMessageError = this.MSG_RC_ERR_CLIENT_NETWORK;
                console.log('A client or network error occurred:', err.message);
            } else {
                // Backend error
                let error: string;
                if (err.error !== null) {
                    error = err.error['error'];
                }
                if (error === this.GROUP_ERROR_ID_NOT_VALID) {
                    this.eventMessageError = this.GROUP_ERROR_ID_NOT_VALID_MSG;
                } else if (error === this.GROUP_ERROR_ID_NOT_FOUND) {
                    this.eventMessageError = this.GROUP_ERROR_ID_NOT_FOUND_MSG;
                } else {
                    this.eventMessageError = `Group server error (HTTP ${err.status}) has occurred.`;
                }
                console.log(`Delete group. The server response status is: ${err.status}. Error message: ` + err.message);
            }
        });
    }

    onGetLoggedInUserTotalGroupContribution(group: Group): number {
        const jGroup = this.getJObj(group);
        const groupContribution = this.loggedInUserGroupContributionList.find(gc => gc.groupId === jGroup.id);
        if (groupContribution === undefined) {
            return 0;
        } else {
            return groupContribution.contribution;
        }
    }

    onCloseEventErrorBox() {
        this.eventMessageError = null;
    }

    getLoggedInUserContributions() {
        this.loggedInUserTotalContribution = 0;
        for (const group of this.groups) {
            let totalGroupContribution = 0;
            const jGroup = this.getJObj(group);
            this.occasionService.getOccasionsForGroup(jGroup.id).subscribe(occasions => {
            for (let i = 0; i < occasions.length; i++) {
                for (const contribution of occasions[i].contributions) {
                    if (contribution['userId'] === this.userId) {
                        totalGroupContribution += contribution['amount'];
                        break;
                    }
                }
            }

            this.loggedInUserGroupContributionList.push(new GroupContribution(jGroup.id, totalGroupContribution));
            this.loggedInUserTotalContribution += totalGroupContribution;
            });
        }
    }

    getGroupsForUser() {
        this.groupService.getGroups(this.userId).subscribe(resp => {
            this.groups = resp.groups;
            this.getLoggedInUserContributions();
        }, err => {
             this.eventMessageError = 'An error occurred obtaining the groups from the server.';
        });
    }

    getJObj(obj): any {
        return JSON.parse(obj);
    }
}
