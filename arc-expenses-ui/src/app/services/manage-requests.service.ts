/*
* created by myrto on 27/4/2018
* */

import { Injectable } from '@angular/core';
import { Request } from '../domain/operation';
import { Observable } from 'rxjs/Observable';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';
import { tempApiUrl } from '../domain/tempAPI';
import {SearchResults} from '../domain/extraClasses';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};



@Injectable()
export class ManageRequestsService {

    apiUrl = `${tempApiUrl}/request/`;

    constructor(private http: HttpClient) {}

    addRequest(newRequest: Request): Observable<Request> {
        const url = `${this.apiUrl}add`;
        console.log(`calling ${url}`);
        console.log(`sending ${JSON.stringify(newRequest)}`);

        return this.http.post<Request>(url, JSON.stringify(newRequest), headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    getRequestById(requestId: string, userEmail: string): Observable<any> {
        const url = `${this.apiUrl}getById/${requestId}`;
        console.log(`calling ${url}`);
        return this.http.get<any>(url, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    updateRequest(updatedRequest: Request, userEmail: string): Observable<Request> {
        const url = `${this.apiUrl}updateRequest`;
        console.log(`calling ${url}`);
        return this.http.post<Request>(url, updatedRequest, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    userCanEditRequest(userEmail: string): Observable<boolean> {
        const url = `${this.apiUrl}auth/${userEmail}`;
        console.log(`calling ${url}`);
        return this.http.get(url, headerOptions).pipe(
            catchError(this.handleError)
        );
    }

    searchAllRequests(keyword: string, from: string, quantity: string,
                      order: string, orderField: string, email: string): Observable<SearchResults<Request>> {
        let url = `${this.apiUrl}getAll?email=${encodeURIComponent(email)}`;

        if (keyword) { url += `&keyword=${keyword}`; }
        if (from) { url += `&from=${from}`; }
        if (quantity) { url += `&quantity=${quantity}`; }
        if (order) { url += `&order=${order}`; }
        if (orderField) { url += `&orderField=${orderField}`; }

        console.log(`calling ${url}`);
        return this.http.get<SearchResults<Request>>(url, headerOptions).pipe(
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
