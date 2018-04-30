import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { deleteCookie, getCookie } from '../domain/cookieUtils';
import {HttpClient} from '@angular/common/http';
import {tempApiUrl, tempLoginApi} from '../domain/tempAPI';


@Injectable()
export class AuthenticationService {

    constructor(private route: ActivatedRoute,
                private router: Router,
                private http: HttpClient) {}

    private apiUrl: string = tempApiUrl;
    private loginUrl: string = tempLoginApi;
    private baseUrl: string = '/';


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
        /*sessionStorage.setItem('state.location', this.router.url);
        window.location.href = this.loginUrl;*/

        /*REMOVE AFTERWARDS*/
        this.isLoggedIn = true;
        this.userId = 'userId';
        this.userEmail = 'email@email.com';
        this.userFirstName = 'firstName';
        this.userLastName = 'lastName';
        this.userFirstNameInLatin = 'firstNameInLatin';
        this.userLastNameInLatin = 'lastNameInLatin';
        this.userRole = 'admin';
        this.getIsUserLoggedIn();
    }

    public logout() {
        /*deleteCookie('currentUser');
        sessionStorage.removeItem('name');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('role');*/
        this.isLoggedIn = false;

        /*console.log('logging out, going to:');
        console.log(`https://aai.openaire.eu/proxy/saml2/idp/SingleLogoutService.php?ReturnTo=${this.baseUrl}`);
        window.location.href = `https://aai.openaire.eu/proxy/saml2/idp/SingleLogoutService.php?ReturnTo=${this.baseUrl}`;*/
        this.router.navigate(['/home']);
    }

    public tryLogin() {
        if (getCookie('currentUser')) {
            console.log(`I got the cookie!`);
            /* SETTING INTERVAL TO REFRESH SESSION TIMEOUT COUNTD */
            setInterval(() => {
                this.http.get(this.loginUrl + '/login',{ withCredentials: true }).subscribe(
                    userInfo => {
                        console.log('User is still logged in');
                        console.log(userInfo);
                        this.isLoggedIn = true;
                    },
                    () => {
                        sessionStorage.removeItem('name');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('role');
                        deleteCookie('currentUser');
                        this.isLoggedIn = false;
                    }
                );
            }, 1000 * 60 * 5);
            if(!sessionStorage.getItem('name')) {
                console.log(`session.name wasn't found --> logging in via repo-service!`);
                this.http.get(this.apiUrl + '/user/login',{ withCredentials: true }).subscribe(
                    userInfo => {
                        console.log(userInfo);
                        sessionStorage.setItem('name', userInfo['name']);
                        sessionStorage.setItem('email', userInfo['email']);
                        sessionStorage.setItem('role', userInfo['role']);
                        this.isLoggedIn = true;
                        console.log(`the current user is: ${sessionStorage.getItem('name')}, ` +
                            `${sessionStorage.getItem('email')}, ${sessionStorage.getItem('role')}`);
                    },
                    () => {
                        sessionStorage.removeItem('name');
                        sessionStorage.removeItem('email');
                        sessionStorage.removeItem('role');
                        deleteCookie('currentUser');
                        this.isLoggedIn = false;
                    }
                );
            } else {
                this.isLoggedIn = true;
                console.log(`the current user is: ${sessionStorage.getItem('name')}, ${sessionStorage.getItem('email')}, ${sessionStorage.getItem('role')}`);
            }
            if ( sessionStorage.getItem('state.location') ) {
                const state = sessionStorage.getItem('state.location');
                sessionStorage.removeItem('state.location');
                console.log(`tried to login - returning to state: ${state}`);
                if (this.redirectUrl) {
                    this.router.navigate([this.redirectUrl]);
                } else {
                    this.router.navigate([state]);
                }
            }
        }
    }

    public getIsUserLoggedIn() {
        return this.isLoggedIn;
    }

    public getUserId() {
        return this.userId;
    }

    public getUserFirstName() {
        /*if (this.isLoggedIn)
            return sessionStorage.getItem('name');
        else
            return '';*/
        return this.userFirstName;
    }

    public getUserLastName() {
        return this.userLastName;
    }

    public getUserFirstNameInLatin() {
        return this.userFirstNameInLatin;
    }

    public getUserLastNameInLatin() {
        return this.userLastNameInLatin;
    }

    public getUserEmail() {
        /*if (this.isLoggedIn)
            return sessionStorage.getItem('email');
        else
            return '';*/
        return this.userEmail;
    }

    public getUserRole() {
        /*if (this.isLoggedIn)
            return sessionStorage.getItem('role');
        else
            return '';*/
        return this.userRole;
    }

}
