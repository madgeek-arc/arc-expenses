import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { deleteCookie, getCookie } from '../domain/cookieUtils';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Attachment } from '../domain/operation';
import { catchError, tap } from 'rxjs/operators';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';
import { environment } from '../../environments/environment';
import { isNullOrUndefined } from 'util';

const headerOptions = {
    headers: new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};

@Injectable()
export class AuthenticationService {

    constructor(private router: Router,
                private http: HttpClient) { }

    private apiUrl: string = environment.API_ENDPOINT;
    private loginUrl: string = environment.API_ENDPOINT + '/user/idp_login';
    private baseUrl: string = environment.HOME_URL;

    // store the URL so we can redirect after logging in
    public redirectUrl: string;

    private _storage: Storage = sessionStorage;

    isLoggedIn: boolean = false;

    public loginWithState() {
        if ( isNullOrUndefined(sessionStorage.getItem('state.location')) ) {
            console.log(`logging in with state. Current url is: ${this.router.url}`);
            sessionStorage.setItem('state.location', sessionStorage.getItem('state.location') );
        }
        console.log(`logging in. Current state.location is: ${this.router.url}`);
        console.log(`going to ${this.loginUrl}`);
        window.location.href = this.loginUrl;
    }

    public logout() {
        deleteCookie('arc_currentUser');
        this.isLoggedIn = false;
        this.removeUserProperties();

        console.log('logging out, going /home');
        console.log(`${this.baseUrl}Shibboleth.sso/Logout?return=${window.location.origin}`);
        window.location.href = `${this.baseUrl}Shibboleth.sso/Logout?return=${window.location.origin}`;
    }

    public tryLogin() {
        console.log('entering tryLogin -> state.location is:', sessionStorage.getItem('state.location'));
        console.log('cookie is:', getCookie('arc_currentUser'));
        if (getCookie('arc_currentUser')) {
            console.log(`I got the cookie!`);

            /* SETTING INTERVAL TO REFRESH SESSION TIMEOUT COUNTD */
            setInterval(() => {
                this.http.get(this.apiUrl + '/user/getUserInfo', headerOptions).subscribe (
                    userInfo => {
                        console.log('User is still logged in');
                        console.log(userInfo);
                        this.setUserProperties(userInfo);
                        this.isLoggedIn = true;
                    },
                    () => {
                        console.log(`Something went wrong -- I'm logging out!`);
                        this.isLoggedIn = false;
                        this.removeUserProperties();
                        deleteCookie('arc_currentUser');
                        this.router.navigate(['/home']);
                    }
                );
            }, 1000 * 60 * 5);
            console.log('email is', this.getUserProp('email'));
            if (!this.getUserProp('email')) {
                console.log(`session.email wasn't found --> logging in via arc-service!`);
                this.http.get(this.apiUrl + '/user/getUserInfo', headerOptions).subscribe(
                    userInfo => {
                        console.log(JSON.stringify(userInfo));
                        this.isLoggedIn = true;
                        sessionStorage.setItem('role', userInfo['role']);
                        /*sessionStorage.setItem('role', 'ROLE_ADMIN');*/
                        this.setUserProperties(userInfo);
                    },
                    error => {
                        console.log('login error!');
                        console.log(error);
                        this.isLoggedIn = false;
                        this.removeUserProperties();
                        deleteCookie('arc_currentUser');
                        this.router.navigate(['/home']);
                    },
                    () => {
                        console.log(`the current user is: ${this.getUserProp('firstname')} ` +
                                    `${this.getUserProp('lastname')}, ` +
                                    `${this.getUserProp('email')}`);
                        /*if ( isNullOrUndefined(sessionStorage.getItem('userInfo')) ||
                             (sessionStorage.getItem('userInfo') === 'null') ) {

                            this.removeUserProperties();
                            deleteCookie('arc_currentUser');
                            this.router.navigate(['/home']);
                        } else {*/
                            if ( (isNullOrUndefined(this.getUserProp('firstname')) ||
                                    (this.getUserProp('firstname') === 'null')) ||
                                isNullOrUndefined(this.getUserProp('lastname')) ||
                                (this.getUserProp('lastname') === 'null')) {

                                console.log('in tryLogin navigating to sign-up');
                                this.router.navigate(['/sign-up']);

                            } else {
                                let state: string;
                                if ( !isNullOrUndefined(sessionStorage.getItem('state.location')) ) {
                                    state = sessionStorage.getItem('state.location');
                                    sessionStorage.removeItem('state.location');
                                } else {
                                    state = '/home';
                                }

                                console.log(`cleared state.location - returning to state: ${state}`);
                                this.router.navigate([state]);
                            }
                        /*}*/
                    }
                );
            } else {
                this.isLoggedIn = true;
            }
        }

    }

    public getIsUserLoggedIn() {
        return this.isLoggedIn;
    }

    public getUserRole() {
        if ( this.isLoggedIn && !isNullOrUndefined(sessionStorage.getItem('role')) ) {
            return sessionStorage.getItem('role');
        } else {
            return '';
        }
    }


    getUserProp(property: string) {
        const user = JSON.parse(sessionStorage.getItem('userInfo'));
        if ( !isNullOrUndefined(user) && !isNullOrUndefined(user[property]) && (user[property] !== 'null') ) {
            /*console.log('read', property, 'it is:', user[property]);*/
            if ( (property === 'immediateEmails') || (property === 'receiveEmails') ) {
                return (user[property] === 'true');
            }
            return user[property];
        }
        return null;
    }

    getSignatureAttachment() {
        const signature: Attachment = this.getUserProp('signatureAttachment');
        if ( this.isLoggedIn && !isNullOrUndefined(signature) ) {

            return signature;
        } else {
            return null;
        }
    }

    getSignatureAttachmentProp( property: string ) {
        const signature: Attachment = this.getUserProp('signatureAttachment');
        if ( this.isLoggedIn && !isNullOrUndefined(signature) &&
             !isNullOrUndefined(signature[property]) && (signature[property] !== 'null') ) {

            return signature[property];
        } else {
            return '';
        }
    }

    setUserProperties (userInfo: any) {
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo));

        /*sessionStorage.setItem('userid', userInfo['uid']);
        sessionStorage.setItem('email', userInfo['email']);
        sessionStorage.setItem('firstname', userInfo['firstname']);
        sessionStorage.setItem('lastname', userInfo['lastname']);
        /!*sessionStorage.setItem('firstname', null);
        sessionStorage.setItem('lastname', null);*!/
        sessionStorage.setItem('firstnameLatin', userInfo['firstnameLatin']);
        sessionStorage.setItem('lastnameLatin', userInfo['lastnameLatin']);
        sessionStorage.setItem('receiveEmails', userInfo['receiveEmails']);
        sessionStorage.setItem('immediateEmails', userInfo['immediateEmails']);
        sessionStorage.setItem('signatureArchiveId', userInfo['signatureArchiveId']);
        if (!isNullOrUndefined(userInfo['signatureAttachment'])) {
            sessionStorage.setItem('signatureFilename', userInfo['signatureAttachment']['filename']);
            sessionStorage.setItem('signatureMimetype', userInfo['signatureAttachment']['mimetype']);
            sessionStorage.setItem('signatureSize', userInfo['signatureAttachment']['size']);
            sessionStorage.setItem('signatureUrl', userInfo['signatureAttachment']['url']);
        }*/
    }

    removeUserProperties () {
        sessionStorage.clear();
        /*sessionStorage.removeItem('userid');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('firstname');
        sessionStorage.removeItem('laststname');
        sessionStorage.removeItem('firstnameLatin');
        sessionStorage.removeItem('lastnameLatin');
        sessionStorage.removeItem('receiveEmails');
        sessionStorage.removeItem('immediateEmails');
        sessionStorage.removeItem('role');
        sessionStorage.removeItem('signatureArchiveId');
        sessionStorage.removeItem('signatureFilename');
        sessionStorage.removeItem('signatureMimetype');
        sessionStorage.removeItem('signatureSize');
        sessionStorage.removeItem('signatureUrl');*/
    }

    updateUserInfo(firstname: string, lastname: string, receiveEmails: string, immediateEmails: string, attachment: Attachment) {

        const url = `${this.apiUrl}/user/update`;
        console.log(`calling ${url}`);

        const updatedUser = {
            email: this.getUserProp('email'),
            firstname: firstname,
            firstnameLatin: this.getUserProp('firstnameLatin'),
            id: this.getUserProp('uid'),
            lastname: lastname,
            lastnameLatin: this.getUserProp('lastnameLatin'),
            receiveEmails: receiveEmails,
            immediateEmails: immediateEmails,
            signatureArchiveId: this.getUserProp('signatureArchiveId'),
            signatureAttachment: attachment
        };

        console.log(`sending: ${JSON.stringify(updatedUser)}`);

        return this.http.post(url, updatedUser, headerOptions).pipe(
            tap(userInfo => {
                if (userInfo) {
                    this.setUserProperties(userInfo);
                }
            }),
            catchError(this.handleError)
        );
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


/*/!* public get  userId() *!/
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

public getUserSignatureArchiveID() {
    if (this.isLoggedIn && !isNullOrUndefined(sessionStorage.getItem('signatureArchiveId')) ) {
        return sessionStorage.getItem('signatureArchiveId');
    } else {
        return '';
    }
}

public getUserSignatureAttachment() {
    if (this.isLoggedIn && !isNullOrUndefined(sessionStorage.getItem('signatureUrl') &&
        (sessionStorage.getItem('signatureUrl') !== 'null') )) {

        const tempAttachment: Attachment = new Attachment();
        tempAttachment.filename = sessionStorage.getItem('signatureFilename');
        tempAttachment.mimetype = sessionStorage.getItem('signatureMimetype');
        tempAttachment.size = +sessionStorage.getItem('signatureSize');
        tempAttachment.url = sessionStorage.getItem('signatureUrl');
        return tempAttachment;
    } else {
        return null;
    }
}*/
