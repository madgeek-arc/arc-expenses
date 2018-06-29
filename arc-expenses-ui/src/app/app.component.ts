import {Component, isDevMode, OnInit} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import { AuthenticationService } from './services/authentication.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'app';

  constructor(public router: Router, private authService: AuthenticationService) {

      this.authService.tryLogin();
  }

  ngOnInit() {
      if (isDevMode()) {
          console.log('In development mode!');
      } else {
          console.log('In production mode!');
      }
      this.router.events.subscribe((evt) => {
          if (!(evt instanceof NavigationEnd)) {
              return;
          }
          window.scrollTo(0, 0);
      });

  }

  showTop() {
    return this.router.url !== '/sign-up';
  }
}
