/*
* created by myrto on 27/4/2018
* */

import { Injectable } from '@angular/core';
import { Request } from '../domain/operation';
import { Observable } from 'rxjs/Observable';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';

let headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json'),
    withCredentials : true
};



@Injectable()
export class ManageRequestsService {

    apiUrl = 'https://aleka.athenarc.gr/arc-expenses-service/request/';

    constructor(private http: HttpClient) {}

    addRequest(newRequest: Request): Observable<Request> {
        let url = `${this.apiUrl}add`;
        return this.http.post<Request>(url, newRequest, headerOptions)
            .pipe(
                catchError(this.handleError)
        );
    }

    /*handleError function as provided by angular.io (copied on 27/4/2018)*/
    private handleError(error: HttpErrorResponse) {
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
    };
}
