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
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { GroupAddMemberComponent } from './group/group-add-member.component';
import { GroupComponent} from './group/group.component';
import { GroupCreateComponent} from './group/group-create.component';
import { GroupsComponent} from './group/groups.component';
import { LoginComponent } from './login/login.component';
import { LogoutComponent } from './logout/logout.component';
import { OccasionCreationComponent } from './occasion/occasion-create.component';
import { OccasionEditComponent } from './occasion/occasion-edit.component';
import { Routes, RouterModule } from '@angular/router';
import { ProfileCreateComponent } from './signup/profile-create.component';
import { ProfileEditComponent } from './signup/profile-edit.component';
import { TwitterLoginComponent } from './login/twitter.login.component';
import { TwitterVerifyComponent } from './login/twitter.verify.component';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'groups', component: GroupsComponent },
    { path: 'groups/group', component: GroupComponent },
    { path: 'groups/group/create', component: GroupCreateComponent},
    { path: 'groups/member/add', component: GroupAddMemberComponent},
    { path: 'login', component: LoginComponent },
    { path: 'logout', component: LogoutComponent },
    { path: 'login/twitter', component: TwitterLoginComponent },
    { path: 'login/twitter/verify', component: TwitterVerifyComponent },
    { path: 'occasions', component: OccasionCreationComponent },
    { path: 'occasions/:id/edit', component: OccasionEditComponent },
    { path: 'signup', component: ProfileCreateComponent },
    { path: 'profiles/:id/edit', component: ProfileEditComponent }
];

export const routing = RouterModule.forRoot(routes);
