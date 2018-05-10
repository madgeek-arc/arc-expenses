import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { deleteCookie, getCookie } from '../domain/cookieUtils';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {tempApiUrl, tempBaseUrl, tempLoginApi} from '../domain/tempAPI';
import { User } from '../domain/extraClasses';
import {catchError, tap} from 'rxjs/operators';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};

@Injectable()
export class AuthenticationService {

    constructor(private route: ActivatedRoute,
                private router: Router,
                private http: HttpClient) {}

    private apiUrl: string = tempApiUrl;
    private loginUrl: string = tempLoginApi;
    private baseUrl: string = tempBaseUrl;


    // store the URL so we can redirect after logging in
    public redirectUrl: string;

    private _storage: Storage = sessionStorage;


    isLoggedIn: boolean = false;
    userId: string;
    userEmail: string;
    userFirstName: string;
    userLastName: string;
    userFirstNameInLatin: string;
    userLastNameInLatin: string;
    userRole: string;

    public loginWithState() {
        console.log(`logging in with state. Current url is: ${this.router.url}`);
        sessionStorage.setItem('state.location', this.router.url);
        console.log(`going to ${this.loginUrl}`);
        window.location.href = this.loginUrl;
    }

    public logout() {
        deleteCookie('arc_currentUser');
        sessionStorage.removeItem('userid');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('firstname');
        sessionStorage.removeItem('laststname');
        sessionStorage.removeItem('firstnameLatin');
        sessionStorage.removeItem('lastnameLatin');
        /*sessionStorage.removeItem('role');*/
        this.isLoggedIn = false;

        /*console.log('logging out, going to:');
        console.log(`https://aai.openaire.eu/proxy/saml2/idp/SingleLogoutService.php?ReturnTo=${this.baseUrl}`);
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
                        sessionStorage.removeItem('userid');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('firstname');
                        sessionStorage.removeItem('laststname');
                        sessionStorage.removeItem('firstnameLatin');
                        sessionStorage.removeItem('lastnameLatin');
                        /*sessionStorage.removeItem('role');*/
                        deleteCookie('arc_currentUser');
                        this.isLoggedIn = false;
                        this.router.navigate(['/home']);
                    }
                );
            }, 1000 * 60 * 5);
            if (!sessionStorage.getItem('email')) {
                console.log(`session.email wasn't found --> logging in via repo-service!`);
                this.http.get(this.apiUrl + '/user/getUserInfo', headerOptions).subscribe(
                    userInfo => {
                        console.log(userInfo);
                        sessionStorage.setItem('userid', userInfo['uid']);
                        sessionStorage.setItem('email', userInfo['email']);
                        sessionStorage.setItem('firstname', userInfo['firstname']);
                        sessionStorage.setItem('lastname', userInfo['lastname']);
                        sessionStorage.setItem('firstnameLatin', userInfo['firstnameLatin']);
                        sessionStorage.setItem('lastnameLatin', userInfo['lastnameLatin']);
                        /*sessionStorage.setItem('role', userInfo['role']);*/
                    },
                    error => {
                        console.log('login error!');
                        console.log(error);
                        sessionStorage.removeItem('userid');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('firstname');
                        sessionStorage.removeItem('laststname');
                        sessionStorage.removeItem('firstnameLatin');
                        sessionStorage.removeItem('lastnameLatin');
                        /*sessionStorage.removeItem('role');*/
                        deleteCookie('arc_currentUser');
                        this.isLoggedIn = false;
                        this.router.navigate(['/home']);
                    },
                    () => {
                        this.isLoggedIn = true;
                        console.log(`the current user is: ${sessionStorage.getItem('firstname')} ` +
                            `${sessionStorage.getItem('lastname')}, ` +
                            `${sessionStorage.getItem('email')}`);

                        let state: string;
                        if ( sessionStorage.getItem('state.location') ) {
                            state = sessionStorage.getItem('state.location');
                            sessionStorage.removeItem('state.location');
                            console.log(`logged in - returning to state: ${state}`);
                        }
                        if (!sessionStorage.getItem('firstname') ||
                            !sessionStorage.getItem('firstname') ||
                            (sessionStorage.getItem('firstname') === 'null') ||
                            (sessionStorage.getItem('lastname') === 'null') ) {
                            console.log('going to sign-up');
                            this.router.navigate(['/sign-up']);
                        } else {
                            if (this.redirectUrl) {
                                this.router.navigate([this.redirectUrl]);
                            } else if (state && state !== '/sign-up') {
                                this.router.navigate([state]);
                            } else {
                                this.router.navigate(['/home']);
                            }
                        }
                    }
                );
            } else {
                this.isLoggedIn = true;
                console.log(`the current user is: ${sessionStorage.getItem('firstname')} ` +
                    `${sessionStorage.getItem('lastname')}, ` +
                    `${sessionStorage.getItem('email')}`);

                if ( sessionStorage.getItem('state.location') ) {
                    const state = sessionStorage.getItem('state.location');
                    sessionStorage.removeItem('state.location');
                    console.log(`tried to login - returning to state: ${state}`);
                    if (!sessionStorage.getItem('firstname') ||
                        !sessionStorage.getItem('firstname') ||
                        (sessionStorage.getItem('firstname') === 'null') ||
                        (sessionStorage.getItem('lastname') === 'null') ) {
                        this.router.navigate(['/sign-up']);
                    } else {
                        if (this.redirectUrl) {
                            this.router.navigate([this.redirectUrl]);
                        } else if (state && state !== '/sign-up') {
                            this.router.navigate([state]);
                        } else {
                            this.router.navigate(['/home']);
                        }
                    }
                }
            }
        }
    }

    updateUserInfo(firstname: string, lastname: string) {
        /*const url = `${this.apiUrl}/user/update?id=${sessionStorage.getItem('userid')}` +
            `&email=${sessionStorage.getItem('email')}` +
            `&firstname=${firstname}` +
            `&lastname=${lastname}` +
            `&firstnameLatin=${sessionStorage.getItem('firstnameLatin')}` +
            `&lastnameLatin=${sessionStorage.getItem('lastnameLatin')}`;*/

        const url = `${this.apiUrl}/user/update`;
        console.log(`calling ${url}`);

        const updatedUser = {
            email: sessionStorage.getItem('email'),
            firstname: firstname,
            firstnameLatin: sessionStorage.getItem('firstnameLatin'),
            id: sessionStorage.getItem('userid'),
            lastname: lastname,
            lastnameLatin: sessionStorage.getItem('lastnameLatin')
        };

        console.log(`sending: ${JSON.stringify(updatedUser)}`);

        return this.http.post(url, updatedUser, headerOptions).pipe (
            tap (userInfo => {
                if (userInfo) {
                    sessionStorage.setItem('firstname', userInfo['firstname']);
                    sessionStorage.setItem('lastname', userInfo['lastname']);
                }
            }),
            catchError(this.handleError)
        );
    }

    public getIsUserLoggedIn() {
        return this.isLoggedIn;
    }

    public getUserId() {
        if (this.isLoggedIn && (sessionStorage.getItem('userid') !== 'null')) {
            return sessionStorage.getItem('userid');
        } else {
            return '';
        }
        /*return this.userId;*/
    }

    public getUserFirstName() {
        if (this.isLoggedIn && (sessionStorage.getItem('firstname') !== 'null')) {
            return sessionStorage.getItem('firstname');
        } else {
            return '';
        }
        /*return this.userFirstName;*/
    }

    public getUserLastName() {
        if (this.isLoggedIn && (sessionStorage.getItem('lastname') !== 'null')) {
            return sessionStorage.getItem('lastname');
        } else {
            return '';
        }
        /*return this.userLastName;*/
    }

    public getUserFirstNameInLatin() {
        if (this.isLoggedIn && (sessionStorage.getItem('firstnameLatin') !== 'null')) {
            return sessionStorage.getItem('firstnameLatin');
        } else {
            return '';
        }
        /*return this.userFirstnameLatin;*/
    }

    public getUserLastNameInLatin() {
        if (this.isLoggedIn && (sessionStorage.getItem('lastnameLatin') !== 'null')) {
            return sessionStorage.getItem('lastnameLatin');
        } else {
            return '';
        }
        /*return this.userLastnameLatin;*/
    }

    public getUserEmail() {
        if (this.isLoggedIn && (sessionStorage.getItem('email') !== 'null')) {
            return sessionStorage.getItem('email');
        } else {
            return '';
        }
        /*return this.userEmail;*/
    }

    public getUserRole() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('role');
        } else {
            return '';
        }
        /*return this.userRole;*/
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
