
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {AuthenticationService} from './authentication.service';
import { getCookie } from '../domain/cookieUtils';
import { isNullOrUndefined } from 'util';

@Injectable ()
export class AuthGuardService implements CanActivate {

    constructor (private authenticationService: AuthenticationService, private router: Router) {}

    canActivate (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        console.log('in authGuard, current url is:', state.url);

        if ( ((getCookie('arc_currentUser') !== null) && (getCookie('arc_currentUser') !== '')) &&
             this.authenticationService.getIsUserLoggedIn() ) {

            return true;
        }

        if ( (getCookie('arc_currentUser') !== null) && (getCookie('arc_currentUser') !== '') ) { return true; }

        // Store the attempted URL for redirecting
        if ( !sessionStorage.getItem('state.location') ) {
            sessionStorage.setItem('state.location', state.url);
        }

        console.log('in authGuard -> going to login!');
        this.authenticationService.loginWithState();

        return false;
    }
}
