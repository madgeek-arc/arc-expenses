import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { TopMenuComponent } from './shared/top-menu/top-menu.component';
import { AppRoutingModule } from './/app-routing.module';
import { NewRequestComponent } from './new-request/new-request.component';
import { HomeComponent } from './home/home.component';
import { RequestsComponent } from './requests/requests.component';
import { AboutComponent } from './about/about.component';
import { SignUpComponent } from './sign-up/sign-up.component';
import {AsideHelpContentComponent, HelpContentComponent} from './shared/help-content/help-content.component';
import {HelpContentService} from './shared/help-content/help-content.service';
import {RequestService} from './request.service';
import { HttpClientModule } from '@angular/common/http';
import { StageFormComponent, StageFormUploadFile } from './shared/stage-form/stage-form.component';
import { RequestStageComponent } from './request-stage/request-stage.component';
import {
    Stage2Component, Stage3Component, Stage3aComponent, StageComponent,
    Stage3bComponent, Stage4Component, Stage5Component, Stage6Component, Stage7Component, Stage8Component,
    Stage9Component, Stage10Component
} from './request-stage/stages-components';

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
    StageFormUploadFile,
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
    Stage10Component
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    HttpClientModule
  ],
  providers: [
    HelpContentService,
    RequestService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
