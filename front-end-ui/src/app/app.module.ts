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
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { HttpModule } from '@angular/http';
import { NgModule } from '@angular/core';
import { MatButtonModule, MatMenuModule, MatIconModule } from '@angular/material';
import { AppComponent } from './app.component';
import { GroupAddMemberComponent } from './group/group-add-member.component';
import { GroupComponent } from './group/group.component';
import { GroupCreateComponent } from './group/group-create.component';
import { GroupService } from './group/services/group.service';
import { GroupsComponent } from './group/groups.component';
import { HeaderComponent } from './header/header.component';
import { LoginComponent } from './login/login.component';
import { LogoutComponent } from './logout/logout.component';
import { OccasionCreationComponent } from './occasion/occasion-create.component';
import { OccasionEditComponent } from './occasion/occasion-edit.component';
import { OccasionService } from './occasion/services/occasion.service';
import { ProfileEditComponent } from './signup/profile-edit.component';
import { ProfileCreateComponent } from './signup/profile-create.component';
import { TwitterLoginComponent } from './login/twitter.login.component';
import { TwitterVerifyComponent } from './login/twitter.verify.component';
import { UserService } from './user/services/user.service';
import { routing } from './app-routing.module';

@NgModule({
    declarations: [
        AppComponent,
        GroupAddMemberComponent,
        GroupComponent,
        GroupCreateComponent,
        GroupsComponent,
        HeaderComponent,
        LoginComponent,
        LogoutComponent,
        OccasionCreationComponent,
        OccasionEditComponent,
        ProfileCreateComponent,
        ProfileEditComponent,
        TwitterLoginComponent,
        TwitterVerifyComponent
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        HttpClientModule,
        HttpModule,
        MatButtonModule,
        MatIconModule,
        MatMenuModule,
        routing
    ],
    providers: [
        GroupService,
        OccasionService,
        UserService],
    bootstrap: [AppComponent]
})

export class AppModule {}
