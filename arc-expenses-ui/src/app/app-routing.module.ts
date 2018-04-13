import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NewRequestComponent } from './new-request/new-request.component';
import {RequestsComponent} from './requests/requests.component';
import {AboutComponent} from './about/about.component';

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
    component: NewRequestComponent
  },
  {
    path: 'requests',
    component: RequestsComponent
  },
  {
    path: 'about',
    component: AboutComponent
  },

];

@NgModule({
  imports: [ RouterModule.forRoot(appRoutes) ],
  exports: [
    RouterModule
  ],
})

export class AppRoutingModule { }
