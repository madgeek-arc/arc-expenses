import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { deleteCookie, getCookie } from '../domain/cookieUtils';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import { Attachment, User } from '../domain/operation';
import {catchError, tap} from 'rxjs/operators';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';
import {environment} from '../../environments/environment';
import { isNullOrUndefined } from 'util';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};

@Injectable()
export class AuthenticationService {

    constructor(private route: ActivatedRoute,
                private router: Router,
                private http: HttpClient) {}

    private apiUrl: string = environment.API_ENDPOINT;
    private loginUrl: string = environment.API_ENDPOINT + '/user/idp_login';
    private baseUrl: string = environment.HOME_URL;


    // store the URL so we can redirect after logging in
    public redirectUrl: string;

    private _storage: Storage = sessionStorage;

    isLoggedIn: boolean = false;

    public loginWithState() {
        console.log(`logging in with state. Current url is: ${this.router.url}`);
        sessionStorage.setItem('state.location', this.router.url);
        console.log(`going to ${this.loginUrl}`);
        window.location.href = this.loginUrl;
    }

    public logout() {
        deleteCookie('arc_currentUser');
        this.isLoggedIn = false;
        sessionStorage.removeItem('userid');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('firstname');
        sessionStorage.removeItem('laststname');
        sessionStorage.removeItem('firstnameLatin');
        sessionStorage.removeItem('lastnameLatin');
        sessionStorage.removeItem('receiveEmails');
        sessionStorage.removeItem('immediateEmails');
        sessionStorage.removeItem('role');

        console.log('logging out, going /home');
        /*console.log(`https://aai.openaire.eu/proxy/saml2/idp/SingleLogoutService.php?ReturnTo=${this.baseUrl}`);
        window.location.href = `https://aai.openaire.eu/proxy/saml2/idp/SingleLogoutService.php?ReturnTo=${this.baseUrl}`;*/
        this.router.navigate(['/home']);
    }

    public tryLogin() {
        if (getCookie('arc_currentUser')) {
            console.log(`I got the cookie!`);
            /* SETTING INTERVAL TO REFRESH SESSION TIMEOUT COUNTD */
            setInterval(() => {
                this.http.get(this.apiUrl + '/user/getUserInfo', headerOptions).subscribe(
                    userInfo => {
                        console.log('User is still logged in');
                        console.log(userInfo);
                        this.isLoggedIn = true;
                    },
                    () => {
                        console.log(`Something went wrong -- I'm logging out!`);
                        this.isLoggedIn = false;
                        sessionStorage.removeItem('userid');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('firstname');
                        sessionStorage.removeItem('laststname');
                        sessionStorage.removeItem('firstnameLatin');
                        sessionStorage.removeItem('lastnameLatin');
                        sessionStorage.removeItem('receiveEmails');
                        sessionStorage.removeItem('immediateEmails');
                        sessionStorage.removeItem('role');
                        deleteCookie('arc_currentUser');
                        this.router.navigate(['/home']);
                    }
                );
            }, 1000 * 60 * 5);
            if (!sessionStorage.getItem('email')) {
                console.log(`session.email wasn't found --> logging in via arc-service!`);
                this.http.get(this.apiUrl + '/user/getUserInfo', headerOptions).subscribe(
                    userInfo => {
                        console.log(JSON.stringify(userInfo));
                        this.isLoggedIn = true;
                        sessionStorage.setItem('userid', userInfo['uid']);
                        sessionStorage.setItem('email', userInfo['email']);
                        sessionStorage.setItem('firstname', userInfo['firstname']);
                        sessionStorage.setItem('lastname', userInfo['lastname']);
                        sessionStorage.setItem('firstnameLatin', userInfo['firstnameLatin']);
                        sessionStorage.setItem('lastnameLatin', userInfo['lastnameLatin']);
                        sessionStorage.setItem('receiveEmails', userInfo['receiveEmails']);
                        sessionStorage.setItem('immediateEmails', userInfo['immediateEmails']);
                        sessionStorage.setItem('role', userInfo['role']);
                        /*sessionStorage.setItem('role', 'ROLE_ADMIN');*/
                    },
                    error => {
                        console.log('login error!');
                        console.log(error);
                        this.isLoggedIn = false;
                        sessionStorage.removeItem('userid');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('firstname');
                        sessionStorage.removeItem('laststname');
                        sessionStorage.removeItem('firstnameLatin');
                        sessionStorage.removeItem('lastnameLatin');
                        sessionStorage.removeItem('receiveEmails');
                        sessionStorage.removeItem('immediateEmails');
                        sessionStorage.removeItem('role');
                        deleteCookie('arc_currentUser');
                        this.router.navigate(['/home']);
                    },
                    () => {
                        console.log(`the current user is: ${sessionStorage.getItem('firstname')} ` +
                            `${sessionStorage.getItem('lastname')}, ` +
                            `${sessionStorage.getItem('email')}`);

                        if ( sessionStorage.getItem('state.location') ) {
                            const stateLoc = sessionStorage.getItem('state.location');
                            sessionStorage.removeItem('state.location');
                            console.log(`logged in - returning to state: ${stateLoc}`);
                        }

                        if (this.isLoggedIn &&
                            (isNullOrUndefined(sessionStorage.getItem('firstname')) ||
                             (sessionStorage.getItem('firstname') === 'null')) ||
                             isNullOrUndefined(sessionStorage.getItem('lastname')) ||
                             (sessionStorage.getItem('lastname') === 'null') ) {

                            this.router.navigate(['/sign-up']);
                        }
                    }
                );
            } else {
                this.isLoggedIn = true;
            }
        }
        let state: string;
        if ( sessionStorage.getItem('state.location') ) {
            state = sessionStorage.getItem('state.location');
            sessionStorage.removeItem('state.location');
            console.log(`cleared session - returning to state: ${state}`);
            this.router.navigate([state]);
        }
        // this.router.navigate(['/home']);
    }

    updateUserInfo(firstname: string, lastname: string, receiveEmails: string, immediateEmails: string, attachment: Attachment) {

        const url = `${this.apiUrl}/user/update`;
        console.log(`calling ${url}`);

        const updatedUser = {
            email: sessionStorage.getItem('email'),
            firstname: firstname,
            firstnameLatin: sessionStorage.getItem('firstnameLatin'),
            id: sessionStorage.getItem('userid'),
            lastname: lastname,
            lastnameLatin: sessionStorage.getItem('lastnameLatin'),
            receiveEmails: receiveEmails,
            immediateEmails: immediateEmails
        };
        // ADD SIGNATURE ATTACHMENT

        console.log(`sending: ${JSON.stringify(updatedUser)}`);

        return this.http.post(url, updatedUser, headerOptions).pipe (
            tap (userInfo => {
                if (userInfo) {
                    sessionStorage.setItem('firstname', userInfo['firstname']);
                    sessionStorage.setItem('lastname', userInfo['lastname']);
                    sessionStorage.setItem('receiveEmails', userInfo['receiveEmails']);
                    sessionStorage.setItem('immediateEmails', userInfo['immediateEmails']);
                }
            }),
            catchError(this.handleError)
        );
    }

    public getIsUserLoggedIn() {
        return this.isLoggedIn;
    }

    /* public get  userId() */
    public getUserId() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('userid')) &&
            (sessionStorage.getItem('userid') !== 'null')) {

            return sessionStorage.getItem('userid');
        } else {
            return '';
        }
    }

    public getUserFirstName() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('firstname')) &&
            (sessionStorage.getItem('firstname') !== 'null')) {

            return sessionStorage.getItem('firstname');
        } else {
            return '';
        }
    }

    public getUserLastName() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('lastname')) &&
            (sessionStorage.getItem('lastname') !== 'null')) {

            return sessionStorage.getItem('lastname');
        } else {
            return '';
        }
    }

    public getUserFirstNameInLatin() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('firstnameLatin')) &&
            (sessionStorage.getItem('firstnameLatin') !== 'null')) {

            return sessionStorage.getItem('firstnameLatin');
        } else {
            return '';
        }
    }

    public getUserLastNameInLatin() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('lastnameLatin')) &&
            (sessionStorage.getItem('lastnameLatin') !== 'null')) {

            return sessionStorage.getItem('lastnameLatin');
        } else {
            return '';
        }
    }

    public getUserEmail() {
        if (this.isLoggedIn &&
            !isNullOrUndefined(sessionStorage.getItem('email')) &&
            (sessionStorage.getItem('email') !== 'null')) {

            return sessionStorage.getItem('email');
        } else {
            return '';
        }
    }

    public getUserReceiveEmails() {
        return (this.isLoggedIn &&
                (sessionStorage.getItem('receiveEmails') &&
                 sessionStorage.getItem('receiveEmails') === 'true'));
    }

    public getUserImmediateEmails() {
        return (this.isLoggedIn &&
            (sessionStorage.getItem('immediateEmails') &&
                sessionStorage.getItem('immediateEmails') === 'true'));
    }

    public getUserRole() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('role');
        } else {
            return '';
        }
    }


    /*handleError function as provided by angular.io (copied on 27/4/2018)*/
    private handleError(error: HttpErrorResponse) {
        console.log(error);
        if (error.error instanceof ErrorEvent) {
            // A client-side or network error occurred. Handle it accordingly.
            console.error('An error occurred:', error.error.message);
        } else {
            // The backend returned an unsuccessful response code.
            // The response body may contain clues as to what went wrong,
            console.error(
                `Backend returned code ${error.status}, ` +
                `body was: ${error.error}`);
        }
        // return an ErrorObservable with a user-facing error message
        return new ErrorObservable(
            'Something bad happened; please try again later.');
    }
}
