import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { deleteCookie, getCookie } from '../domain/cookieUtils';
import {HttpClient} from '@angular/common/http';
import {tempApiUrl, tempBaseUrl, tempLoginApi} from '../domain/tempAPI';


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
        window.location.href = this.loginUrl;
    }

    public logout() {
        deleteCookie('arc_currentUser');
        sessionStorage.removeItem('userid');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('firstname');
        sessionStorage.removeItem('laststname');
        sessionStorage.removeItem('firstnameinlatin');
        sessionStorage.removeItem('lastnameinlatin');
        sessionStorage.removeItem('role');

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
                this.http.get(this.apiUrl + '/user/getUserInfo',{ withCredentials: true }).subscribe(
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
                        sessionStorage.removeItem('firstnameinlatin');
                        sessionStorage.removeItem('lastnameinlatin');
                        sessionStorage.removeItem('role');
                        deleteCookie('arc_currentUser');
                        this.isLoggedIn = false;
                    }
                );
            }, 1000 * 60 * 5);
            if (!sessionStorage.getItem('email')) {
                console.log(`session.name wasn't found --> logging in via repo-service!`);
                this.http.get(this.apiUrl + '/user/getUserInfo',{ withCredentials: true }).subscribe(
                    userInfo => {
                        console.log(userInfo);
                        sessionStorage.setItem('userid', userInfo['userid']);
                        sessionStorage.setItem('email', userInfo['email']);
                        sessionStorage.setItem('firstname', userInfo['firstname']);
                        sessionStorage.setItem('lastname', userInfo['laststname']);
                        sessionStorage.setItem('firstnameinlatin', userInfo['firstnameinlatin']);
                        sessionStorage.setItem('lastnameinlatin', userInfo['lastnameinlatin']);
                        sessionStorage.setItem('role', userInfo['role']);
                        this.isLoggedIn = true;
                        console.log(`the current user is: ${sessionStorage.getItem('firstname')}` +
                            `${sessionStorage.getItem('lastname')}, ` +
                            `${sessionStorage.getItem('email')}, ${sessionStorage.getItem('role')}`);
                    },
                    () => {
                        sessionStorage.removeItem('userid');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('firstname');
                        sessionStorage.removeItem('laststname');
                        sessionStorage.removeItem('firstnameinlatin');
                        sessionStorage.removeItem('lastnameinlatin');
                        sessionStorage.removeItem('role');
                        deleteCookie('arc_currentUser');
                        this.isLoggedIn = false;
                    }
                );
            } /*else {
                this.isLoggedIn = true;
                console.log(`the current user is: ${sessionStorage.getItem('firstname')}` +
                    `${sessionStorage.getItem('lastname')}, ` +
                    `${sessionStorage.getItem('email')}, ${sessionStorage.getItem('role')}`);
            }*/
            if ( sessionStorage.getItem('state.location') ) {
                const state = sessionStorage.getItem('state.location');
                sessionStorage.removeItem('state.location');
                console.log(`tried to login - returning to state: ${state}`);
                /*if (this.redirectUrl) {
                    this.router.navigate([this.redirectUrl]);
                } else {*/
                    this.router.navigate([state]);
                /*}*/
            }
        }
    }

    public getIsUserLoggedIn() {
        return this.isLoggedIn;
    }

    public getUserId() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('userid');
        } else {
            return '';
        }
        /*return this.userId;*/
    }

    public getUserFirstName() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('firstname');
        } else {
            return '';
        }
        /*return this.userFirstName;*/
    }

    public getUserLastName() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('lastname');
        } else {
            return '';
        }
        /*return this.userLastName;*/
    }

    public getUserFirstNameInLatin() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('firstnameinlatin');
        } else {
            return '';
        }
        /*return this.userFirstNameInLatin;*/
    }

    public getUserLastNameInLatin() {
        if (this.isLoggedIn) {
            return sessionStorage.getItem('lastnameinlatin');
        } else {
            return '';
        }
        /*return this.userLastNameInLatin;*/
    }

    public getUserEmail() {
        if (this.isLoggedIn) {
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

}
