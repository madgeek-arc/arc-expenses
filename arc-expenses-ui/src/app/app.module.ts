import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { TopMenuComponent } from './shared/top-menu/top-menu.component';
import { AppRoutingModule } from './app-routing.module';
import { NewRequestComponent } from './new-request/new-request.component';
import { HomeComponent } from './home/home.component';
import { RequestsComponent } from './requests/requests.component';
import { AboutComponent } from './about/about.component';
import { SignUpComponent } from './sign-up/sign-up.component';
import {AsideHelpContentComponent, HelpContentComponent} from './shared/help-content/help-content.component';
import {HelpContentService} from './shared/help-content/help-content.service';
import { HttpClientModule } from '@angular/common/http';
import { StageFormComponent, StageFormUploadFileComponent } from './shared/stage-form/stage-form.component';
import { RequestStageComponent } from './request-stage/request-stage.component';
import {
    Stage2Component, Stage3Component, Stage3aComponent, StageComponent,
    Stage3bComponent, Stage4Component, Stage5Component, Stage6Component, Stage7Component, Stage8Component,
    Stage9Component, Stage10Component
} from './request-stage/stages-components';
import { ManageRequestsService } from './services/manage-requests.service';
import {AuthenticationService} from './services/authentication.service';
import {AuthGuardService} from './services/auth-guard.service';
import {ManageProjectService} from './services/manage-project.service';

import {LOCALE_ID} from '@angular/core';
import {CommonModule, registerLocaleData} from '@angular/common';
import localeEL from '@angular/common/locales/el';
import {FilterByTerm} from './shared/search-term.pipe';

registerLocaleData(localeEL);

@NgModule({
  declarations: [
    AppComponent,
    TopMenuComponent,
    NewRequestComponent,
    HomeComponent,
    RequestsComponent,
    AboutComponent,
    SignUpComponent,
    HelpContentComponent,
    AsideHelpContentComponent,
    StageFormComponent,
    StageFormUploadFileComponent,
    RequestStageComponent,
    StageComponent,
    Stage2Component,
    Stage3Component,
    Stage3aComponent,
    Stage3bComponent,
    Stage4Component,
    Stage5Component,
    Stage6Component,
    Stage7Component,
    Stage8Component,
    Stage9Component,
    Stage10Component,
    FilterByTerm
  ],
  imports: [
    CommonModule,
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    HttpClientModule
  ],
  providers: [
    { provide: LOCALE_ID, useValue: 'el' },
    HelpContentService,
    ManageRequestsService,
    ManageProjectService,
    AuthenticationService,
    AuthGuardService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
