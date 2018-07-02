import {Component, isDevMode, OnInit} from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { AuthenticationService } from './services/authentication.service';
import { isNullOrUndefined } from 'util';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'app';

  constructor(private router: Router, private route: ActivatedRoute,
              private authService: AuthenticationService) {

      if ( isNullOrUndefined(sessionStorage.getItem('state.location')) &&
           !this.authService.getIsUserLoggedIn() ) {
          sessionStorage.setItem('state.location', this.router.url);
      }
      this.authService.tryLogin();
  }

  ngOnInit() {
      /*console.log('in app component', this.route.snapshot.url.slice(-1, 1));*/
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
