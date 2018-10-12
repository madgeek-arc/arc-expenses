import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NewRequestComponent } from './new-request/new-request.component';
import {RequestsComponent} from './requests/requests.component';
import {AboutComponent} from './about/about.component';
import {SignUpComponent} from './sign-up/sign-up.component';
import {RequestStageComponent} from './request-stage/request-stage.component';
import {AuthGuardService} from './services/auth-guard.service';
import { ForbiddenPageComponent } from './shared/403-forbidden-page.component';
import {AdminPageComponent} from './admin-page/admin-page.component';
import { RequestStagePaymentComponent } from './request-stage/request-stage-payment/request-stage-payment.component';

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
    path: 'new-request/regular',
    canActivate: [AuthGuardService],
    component: NewRequestComponent
  },
  {
      path: 'new-request/trip',
      canActivate: [AuthGuardService],
      component: NewRequestComponent
  },
  {
      path: 'new-request/contract',
      canActivate: [AuthGuardService],
      component: NewRequestComponent
  },
  {
      path: 'new-request/services_contract',
      canActivate: [AuthGuardService],
      component: NewRequestComponent
  },
  {
    path: 'requests',
    canActivate: [AuthGuardService],
    children: [
        { path: '', component: RequestsComponent },
        { path: 'request-stage/:id', component: RequestStageComponent },
        { path: 'request-stage-payment/:id', component: RequestStagePaymentComponent }
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
    path: 'admin',
    canActivate: [AuthGuardService],
    canLoad: [AuthGuardService],
    component: AdminPageComponent
  },
  {
      path: '403-forbidden',
      component: ForbiddenPageComponent
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
