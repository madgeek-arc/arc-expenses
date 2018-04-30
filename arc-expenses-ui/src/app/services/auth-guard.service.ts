
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {AuthenticationService} from './authentication.service';
import { getCookie } from '../domain/cookieUtils';

@Injectable ()
export class AuthGuardService implements CanActivate {

    private loginUrl = 'some.login.url';

    constructor (private authenticationService: AuthenticationService, private router: Router) {}

    canActivate (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {

        if ( this.authenticationService.getIsUserLoggedIn() ) { return true; }

        /*uncomment when cookie is used*/
        /*if ( getCookie('currentUser') != null ) { return true; }*/

        // Store the attempted URL for redirecting
        /*sessionStorage.setItem('state.location', state.url);*/

        // Navigate to the login page via the API
        /*window.location.href = this.loginUrl;*/
        this.router.navigate(['/home']);

        return false;
    }
}
