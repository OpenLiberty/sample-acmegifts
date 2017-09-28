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
import { GroupService } from './services/group.service';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { User } from '../user/user';
import { UserService } from '../user/services/user.service';
import { HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-group-create',
  templateUrl: './group-create.component.html',
  providers: [GroupService, UserService]
})
export class GroupCreateComponent implements OnInit {
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.';
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_RC_ERR_CLIENT_NETWORK = 'Network connectivity or client error';

    userId: string;
    id: string;
    group = new Group('', '', []);
    user: User = new User('', '', '', '', '', '', '', '');
    userGroups: Group[] = [];
    eventMessageError: string = null;

    constructor(private route: ActivatedRoute,
            private groupService: GroupService,
            private userService: UserService,
            private router: Router) {
    }

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
            this.groupService.getGroups(this.userId).subscribe(resp => {
                this.userGroups = resp.groups;
            }, err => {
                this.eventMessageError = 'An error occurred obtaining the groups from the server.';
            });
        });
    }

    onCreateGroup(): void {
        this.group.members = [this.userId];

        // Validate that the group name does not already exist.
        const existingGroup = this.userGroups.find(existing => {
            return ((JSON.parse(<any>(existing)).name.toLowerCase()) === (this.group.name.toLowerCase()));
        });

        if (existingGroup !== undefined) {
            this.eventMessageError = 'Group with the name of ' + this.group.name + ' already exists. Please enter a new name.';
            this.group.name = '';
            return;
        }
        const payload = JSON.stringify(this.group);
        this.groupService.createGroup(payload).subscribe(resp => {
            this.id = resp['id']; 
            this.routeToGroupsView();
        }, (err: HttpErrorResponse) => {
            if (err.error instanceof Error) {
                // Client error or network error.
                this.eventMessageError = this.MSG_RC_ERR_CLIENT_NETWORK;
                console.log('A client or network error occurred:', err.message);
            } else {
                console.log(`Create group. The server response status is: ${err.status}. Error message: ` + err.message);
                this.eventMessageError = `Group server error (HTTP ${err.status}) has occurred.`;
            }
        });
    }

    onCancel(): void {
        this.group = new Group('', '', []);
        this.routeToGroupsView();
    }

    onCloseEventBox() {
        this.eventMessageError = null;
    }

    routeToGroupsView(): void {
        this.router.navigate([ '/groups', {userId: this.userId}]);
    }
}
