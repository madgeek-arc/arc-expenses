
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, CanLoad, Router, RouterStateSnapshot} from '@angular/router';
import {AuthenticationService} from './authentication.service';
import { getCookie } from '../domain/cookieUtils';
import { isNullOrUndefined } from 'util';

@Injectable ()
export class AuthGuardService implements CanActivate, CanLoad {

    constructor (private authenticationService: AuthenticationService, private router: Router) {}

    canActivate (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        console.log('in authGuard, current url is:', state.url);

        /*if ( ((getCookie('arc_currentUser') !== null) && (getCookie('arc_currentUser') !== '')) &&
             this.authenticationService.getIsUserLoggedIn() ) {*/
        if ( this.authenticationService.getIsUserLoggedIn() ) {

            return true;
        }
        //console.log('searching for cookie and checking if user is null');
        //if ( getCookie('arc_currentUser') && !this.authenticationService.getUserProp('email') ) { return false; }

        if ( getCookie('arc_currentUser') ) { return true; }

        // Store the attempted URL for redirecting
        if ( !sessionStorage.getItem('state.location') ) {
            sessionStorage.setItem('state.location', state.url);
        }

        console.log('in authGuard -> going to login!');
        this.authenticationService.loginWithState();

        return false;
    }

    canLoad () {

        if ( this.authenticationService.getUserRole() === 'ROLE_ADMIN' ) {

            return true;
        }

        this.router.navigate(['/home']);

        return false;
    }
}
