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
    AsideHelpContentComponent
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
