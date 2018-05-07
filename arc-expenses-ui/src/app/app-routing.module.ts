import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NewRequestComponent } from './new-request/new-request.component';
import {RequestsComponent} from './requests/requests.component';
import {AboutComponent} from './about/about.component';
import {SignUpComponent} from './sign-up/sign-up.component';
import {RequestStageComponent} from './request-stage/request-stage.component';
import {AuthGuardService} from './services/auth-guard.service';

const appRoutes: Routes = [
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    component: HomeComponent
  },
  {
    path: 'new-request',
    //canActivate: [AuthGuardService],
    component: NewRequestComponent
  },
  {
    path: 'requests',
    canActivate: [AuthGuardService],
    children: [
        { path: '', component: RequestsComponent },
        { path: 'request-stage/:id', component: RequestStageComponent }
      ]
  },
  {
    path: 'about',
    component: AboutComponent
  },
  {
    path: 'sign-up',
    canActivate: [AuthGuardService],
    component: SignUpComponent
  },
  {
    path: '**',
    redirectTo: '/home'
  }

];

@NgModule({
  imports: [ RouterModule.forRoot(appRoutes) ],
  exports: [
    RouterModule
  ],
})

export class AppRoutingModule { }
