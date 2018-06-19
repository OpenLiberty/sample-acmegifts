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
import { DomSanitizer } from '@angular/platform-browser';
import { Group } from './group';
import { GroupService } from './services/group.service';
import { HttpErrorResponse, HttpResponse} from '@angular/common/http';
import { MatIconRegistry } from '@angular/material';
import { Occasion } from '../occasion/Occasion';
import { OccasionService } from '../occasion/services/occasion.service';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { User } from '../user/user';
import { UserService } from '../user/services/user.service';
import { Users } from '../user/users';

@Component({
    selector: 'app-group',
    templateUrl: './group.component.html',
    providers: [GroupService, UserService]
})
export class GroupComponent implements OnInit {
    MSG_USER_NOT_RETRIEVED = 'User information could not be retrieved.';
    MSG_ONE_OR_MORE_USERS_NOT_RETRIEVED = 'The information of one or more users could not be retrieved.';
    MSG_GRPNAME_EDIT_BAD_NAME = 'Please enter a valid alphanumeric group name. Maximum size: 30 characters.';
    MSG_USER_ID_INVALID = 'Your session has become invalid. Please login again.';
    MSG_GRP_ID_INVALID = 'The group you attempted to view is not known. Please, refresh your list of groups and try again.';
    MSG_RC_ERR_CLIENT_NETWORK = 'Network connectivity or client error';

    GROUP_ERROR_ID_NOT_VALID = 'The group ID is not valid';
    GROUP_ERROR_ID_NOT_FOUND = 'The group was not found';
    GROUP_ERROR_ID_NOT_VALID_MSG = 'The group service returned an error indicating that the group ID was not valid.';
    GROUP_ERROR_ID_NOT_FOUND_MSG = 'The group service returned an error indicating that the group was not found.';
    GROUP_ERROR_OBTAINING_GROUP_MSG = 'An error occurred obtaining the group from the server';

    userId: string;
    groupId: string;
    group: Group = new Group('', '', []);
    user: User = new User('', '', '', '', '', '', '', '');
    addedMembers: User[] = [];
    occasions: Occasion[];
    startGroupNameEdit = false;
    editableGroupName: string;
    eventMessageError: string = null;
    eventMessageSuccess: string = null;

    constructor(private route: ActivatedRoute,
        private groupService: GroupService,
        private iconRegistry: MatIconRegistry,
        private occasionService: OccasionService,
        private userService: UserService,
        private sanitizer: DomSanitizer,
        private router: Router) {
        // Need to register the icon that we'll use to display the menu.
        // This is required by the material code that
        // we use to display the menu (icon).
        iconRegistry.addSvgIcon(
            'hamburger',
            sanitizer.bypassSecurityTrustResourceUrl('assets/images/icon_menu.svg'));
    }

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
            this.getGroupData();

            // Get the current group's occasions
            this.getOccasions();

            // Get the current user's data.
            this.userService.getUser(this.userId).subscribe(resp => {
                    this.user = resp;
                    sessionStorage.userName = this.user.userName;
                }, err => {
                    // Report the error and stay on the same page.
                    sessionStorage.userName = '';
                    this.eventMessageError = this.MSG_USER_NOT_RETRIEVED;
                });
        });
    }

    onAddMember(search: string) {
        this.router.navigate([ 'groups/member/add', {groupId: this.groupId, userId: this.userId}]);
    }

    onAddOccasion() {
        this.router.navigate([ '/occasions', {userId: this.userId, groupId: this.groupId}]);
    }

    onDelete(): void {
        this.groupService.deleteGroup(this.groupId).subscribe(resp => {
            this.routeToGroupsView();
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

    onDeleteMember(member: User) {
        const index = this.group.members.indexOf(member.id);
        if (index > -1) {
            // Remove the member (id) from the group.
            this.group.members.splice(index, 1);
            const payload = JSON.stringify(this.group);
            this.groupService.updateGroup(this.groupId, payload).subscribe(resp => {

                // Remove the member data (User) from our current list.
                for (let i = 0; i < this.addedMembers.length; i++) {
                    if (member.id === this.addedMembers[i].id) {
                        this.addedMembers.splice(i, 1);
                    }
                }

                // Refresh the group data.
                this.groupService.getGroup(this.groupId).subscribe(getGroupResp => {
                    this.group = getGroupResp;
                }, err => {
                    this.eventMessageError = this.GROUP_ERROR_OBTAINING_GROUP_MSG;
                });
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
                    console.log(`Delete member. The server response status is: ${err.status}. Error message: ` + err.message);
                }
                // Refresh the group data.
                this.groupService.getGroup(this.groupId).subscribe(resp => {
                    this.group = resp;
                }, getGroupErr => {
                    this.eventMessageError = this.GROUP_ERROR_OBTAINING_GROUP_MSG;
                });
            });
        }
    }

    onDeleteOccasion(occasion: Occasion) {
        this.occasionService.deleteOccasion(occasion._id).subscribe(resp => { this.getOccasions(); });
    }

    onEditOccasion(occasion: Occasion) {
        this.router.navigate([ '/occasions', occasion._id, 'edit', { userId: this.userId, groupId: this.groupId } ]);
    }

    onRunOccasion(occasion: Occasion) {
        this.occasionService.runOccasion(occasion).subscribe((res: HttpResponse<any>) => {
            const runSuccessMessage: string = res.body['runSuccess'];
            const runErrorMessage: string = res.body['runError'];
            // Check for success or error
            if (runSuccessMessage === null || runSuccessMessage === undefined  || runSuccessMessage === '' ) {
                if (runErrorMessage === null || runErrorMessage === undefined  || runErrorMessage === '' ) {
                    this.eventMessageSuccess = null;
                    this.eventMessageError = 'Notification request did not return a valid response.';
                } else {
                    this.eventMessageSuccess = null;
                    this.eventMessageError = runErrorMessage;
                }
            } else {
                this.eventMessageSuccess = runSuccessMessage;
                this.eventMessageError = null;
            }

            // Update the occasion list after the occasion runs.  It runs
            // synchronously so the occasion we just ran should be
            // removed from the list.
            this.getOccasions();
        }, (err: HttpErrorResponse) => {
            console.log('Run now encountered an Http error');
            this.eventMessageError = 'Notification request Http error.';
            this.eventMessageSuccess = null;

            // Update the occasion list in case something changed.
            this.getOccasions();
        });
    }

    onCloseEventSuccessBox() {
         this.eventMessageSuccess = null;
    }

    onCloseEventErrorBox() {
        this.eventMessageError = null;
    }

    onStartGroupNameEdit() {
        document.getElementById('flexInput').style.width = '317px';
        this.startGroupNameEdit = true;
    }

    onSaveGroupNameEdit() {
        if (/^\s*$/.test(this.editableGroupName)) {
            this.eventMessageError = this.MSG_GRPNAME_EDIT_BAD_NAME;
            return;
        }
        this.editableGroupName = this.editableGroupName.trim();

        const groupNameLength = this.getPixelLength('25px IBMPlexSans', this.editableGroupName);
        document.getElementById('flexInput').style.width = groupNameLength + 'px';

        let saveGroupName: string = this.group.name;
        this.group.name = this.editableGroupName;
        const payload = JSON.stringify(this.group);
        this.groupService.updateGroup(this.groupId, payload).subscribe(resp => {
            this.startGroupNameEdit = false;
            this.eventMessageError = null;
        }, (err: HttpErrorResponse) => {
            this.group.name = saveGroupName;
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
                console.log(`Group name edit. The server response status is: ${err.status}. Error message: ` + err.message);
            }
        });
    }

    onCancelGroupNameEdit() {
        this.editableGroupName = this.group.name;

        const groupNameLength = this.getPixelLength('25px IBMPlexSans', this.editableGroupName);
        document.getElementById('flexInput').style.width = groupNameLength + 'px';

        this.startGroupNameEdit = false;
        this.eventMessageError = null;
    }

    onGetLoggedInUserContributionAmount(occasion: Occasion): number {
        for (const contribution of occasion.contributions) {
            if (contribution['userId'] === this.userId) {
                return contribution['amount'];
            }
        }
    }

    onBackToGroups() {
        this.routeToGroupsView();
    }

    getMemberData() {
        let errorGettingUserDetected = false;
        for (const memberId of this.group.members) {
            if (this.userId !== memberId) {
               this.userService.getUser(memberId).subscribe(resp => { 
                       this.addedMembers.push(resp); 
                   }, err => {
                       errorGettingUserDetected = true;
                   });
            }
        }
        
        // If there were some errors getting user information, report the 
        // error and stay on the same page.
        if (errorGettingUserDetected) {
            this.eventMessageError = this.MSG_ONE_OR_MORE_USERS_NOT_RETRIEVED;
        }
    }

    getOccasions() {
        this.occasionService.getOccasionsForGroup(this.groupId).subscribe(resp => { this.occasions = resp; });
    }

    getGroupData() {
        this.groupService.getGroup(this.groupId).subscribe(resp => {
            this.group = resp; 
            this.getMemberData();
            this.editableGroupName = this.group.name;

            const groupNameLength = this.getPixelLength('25px IBMPlexSans', this.editableGroupName);
            document.getElementById('flexInput').style.width = groupNameLength + 'px';
        }, err => {
            this.eventMessageError = this.GROUP_ERROR_OBTAINING_GROUP_MSG;
        });
    }


    routeToGroupsView(): void {
        this.router.navigate([ '/groups', {userId: this.userId}]);
    }

    getPixelLength(fontType: string, stringToMeasure: string): number {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        ctx.font = fontType;
        return ctx.measureText(stringToMeasure).width;
    }
}
