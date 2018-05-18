
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {AuthenticationService} from './authentication.service';
import { getCookie } from '../domain/cookieUtils';

@Injectable ()
export class AuthGuardService implements CanActivate {

    constructor (private authenticationService: AuthenticationService, private router: Router) {}

    canActivate (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {

        if ( this.authenticationService.getIsUserLoggedIn() ) { return true; }

        /*uncomment when cookie is used*/
        if ( getCookie('arc_currentUser') != null ) { return true; }

        // Store the attempted URL for redirecting
        /*sessionStorage.setItem('state.location', state.url);*/

        // Navigate to the home page page
        // this.router.navigate(['/home']);
        console.log('in authGuard -> going to login!');
        this.authenticationService.loginWithState();

        return false;
    }
}
