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
import { NgModule } from '@angular/core';
import { User } from '../user/user';
import { Users } from '../user/users';
import { UserService } from '../user/services/user.service';
import { Group } from './group';
import { GroupService } from './services/group.service';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { HttpErrorResponse} from '@angular/common/http';

@Component({
    selector: 'app-group-member-search',
    templateUrl: './group-add-member.component.html',
    providers: [UserService, GroupService]
})
export class GroupAddMemberComponent implements OnInit {
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.';
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_GRP_ID_INVALID = 'The group you attempted to view is not known. Please, refresh your list of groups and try again.';
    MSG_RC_ERR_CLIENT_NETWORK = 'Network connectivity or client error';

    GROUP_ERROR_ID_NOT_VALID = 'The group ID is not valid';
    GROUP_ERROR_ID_NOT_FOUND = 'The group was not found';
    GROUP_ERROR_ID_NOT_VALID_MSG = 'The group service returned an error indicating that the group ID was not valid.';
    GROUP_ERROR_ID_NOT_FOUND_MSG = 'The group service returned an error indicating that the group was not found.';

    group: Group;
    groupId: string;
    userOptions: string[] = [];
    matchedOptions = [];
    query = '';
    user: User = new User('', '', '', '', '', '', '', '');
    userId: string;
    userMap: Map<string, User>;
    eventMessageError: string = null;


    constructor(private route: ActivatedRoute,
                private userService: UserService,
                private groupService: GroupService,
                private router: Router) {}

    ngOnInit() {
        this.route.paramMap.subscribe(params => {
            this.userId = params.get('userId');

            // If we did not receive a user id, the JWT token is, most likely, expired.
            if (this.userId === '' || this.userId === undefined || this.userId === null) {
                this.eventMessageError = this.MSG_USER_ID_INVALID;
                return;
            }

            this.groupId = params.get('groupId');

            // If we did not receive a group id, the group has been, most likely, deleted.
            if (this.groupId === '' || this.groupId === undefined || this.groupId === null) {
                this.eventMessageError = this.MSG_GRP_ID_INVALID;
                return;
            }

            // Get the current group's data.
            this.groupService.getGroup(this.groupId).subscribe(resp => {
                this.group = resp; 
            }, err => {
                this.eventMessageError = 'An error occurred obtaining the group from the server.';
            });
            

            // Get the current user's data.
            this.userService.getUser(this.userId).subscribe(resp => {
                    this.user = resp; 
                    sessionStorage.userName = this.user.userName;
                }, err => {
                    // Report the error and stay on the same page.
                    sessionStorage.userName = '';
                    this.eventMessageError = this.MSG_USER_NOT_RETRIEVED;
            });

            // Get a list of all users.
            this.getAllUsers();
        });
    }

    onFilter() {
        if (this.query === '') {
            this.matchedOptions = [];
            return;
        }

        this.matchedOptions = this.userOptions.filter(
            function(option) {
                return option.toLowerCase().indexOf(this.query.toLowerCase()) >= 0;
            }.bind(this));
    }

    onAddMember(selectedUser: string) {
        this.matchedOptions = [];
        const group = new Group(this.group.id, this.group.name, this.group.members);
        const userName = selectedUser.substring(selectedUser.indexOf('(') + 1, selectedUser.length - 1);
        const user = this.userMap.get(userName);

        // Validate that the selected user is not already a member.
        const existingUser = group.members.find(existing => existing === user.id);

        if (existingUser !== undefined) {
            this.eventMessageError = 'User ' + selectedUser + ' is already a member of this group. Please select another user.';
            this.query = '';
            return;
        }

        // Add the user to the member list.
        group.members.push(user.id);
        const payload = JSON.stringify(group);
        this.groupService.updateGroup(this.groupId, payload).subscribe(resp => {
            this.routeToGroupView();
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
                console.log(`Add member. The server response status is: ${err.status}. Error message: ` + err.message);
            }
        });
    }

    onCancel() {
        this.routeToGroupView();
    }

    onCloseEventBox() {
        this.eventMessageError = null;
    }

    routeToGroupView() {
        this.router.navigate([ 'groups/group', {groupId: this.groupId, userId: this.userId}]);
    }

    getAllUsers() {
        this.userMap = new Map<string, User>();
        this.userService.getUsers().subscribe(resp => {
            resp.users.forEach((el) => {
                const user: User = el;
                this.userOptions.push(user.firstName + ' '  + user.lastName + ' (' + user.userName + ')');
                this.userMap.set(user.userName, el);
            });
        }, err => {
            // Stay here and report an error.
            this.eventMessageError = this.MSG_USER_NOT_RETRIEVED;
        });
    }
}
