import { Component, OnInit } from '@angular/core';
import {AuthenticationService} from '../../services/authentication.service';

@Component({
  selector: 'app-top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.scss']
})
export class TopMenuComponent implements OnInit {

  loggedIn: boolean = false;

  constructor(private authService: AuthenticationService) { }

  ngOnInit() {
    this.isUserLoggedIn();
  }

  login() {
    if (!this.isUserLoggedIn()) {
        this.authService.loginWithState();
        /*this.loggedIn = true;*/
    } else {
      this.logout();
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
    return this.authService.getUserFirstNameInLatin() + ' ' + this.authService.getUserLastNameInLatin();
  }

}
