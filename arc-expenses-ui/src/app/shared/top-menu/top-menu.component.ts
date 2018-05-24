import { Component, OnInit } from '@angular/core';
import {AuthenticationService} from '../../services/authentication.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.scss']
})
export class TopMenuComponent implements OnInit {

  loggedIn: boolean = false;

  constructor(private authService: AuthenticationService, private router: Router) { }

  ngOnInit() {
    this.isUserLoggedIn();
  }

  login() {
    if (!this.isUserLoggedIn()) {
        this.authService.loginWithState();
        /*this.loggedIn = true;*/
    }
  }

  logout() {
    this.authService.logout();
    /*this.loggedIn = false;*/
  }

  isUserLoggedIn() {
    return this.authService.getIsUserLoggedIn();
  }

  getUserName() {
    if ( this.authService.getIsUserLoggedIn() && (!this.authService.getUserFirstName() || !this.authService.getUserLastName()) ) {
        this.router.navigate(['/sign-up']);
    }
    return this.authService.getUserFirstName() + ' ' + this.authService.getUserLastName();
  }

}
