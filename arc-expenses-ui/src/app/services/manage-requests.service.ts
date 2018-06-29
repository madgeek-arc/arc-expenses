/*
* created by myrto on 27/4/2018
* */

import { Injectable } from '@angular/core';
import { Request } from '../domain/operation';
import { Observable } from 'rxjs/Observable';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpHeaders, HttpRequest } from '@angular/common/http';
import { catchError, tap } from 'rxjs/operators';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';
import { Paging } from '../domain/extraClasses';
import { ContactUsMail } from '../domain/operation';
import {environment} from '../../environments/environment';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};



@Injectable()
export class ManageRequestsService {

    apiUrl = environment.API_ENDPOINT + '/request/';

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
        return this.http.get<any>(url, headerOptions);
    }

    updateRequest(updatedRequest: Request, userEmail: string): Observable<Request> {
        const url = `${this.apiUrl}updateRequest`;
        console.log(`calling ${url}`);
        return this.http.post<Request>(url, updatedRequest, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    isEditable(req: Request, email: string): Observable<boolean> {
        const url = `${this.apiUrl}isEditable?email=${encodeURIComponent(email)}`;
        console.log(`calling ${url}`);
        // console.log(`sending ${JSON.stringify(req)}`);
        return this.http.post<boolean>(url, JSON.stringify(req), headerOptions).pipe(
            catchError(this.handleError)
        );
    }

    uploadAttachment<T>(archiveid: string, stage: string, file: File): Observable<HttpEvent<T>> {
        const url = `${this.apiUrl}store/uploadFile?archiveID=${archiveid}&stage=${stage}`;
        console.log(`calling ${url}`);

        const formBody: FormData = new FormData();
        formBody.append('file', file, file.name);
        const req = new HttpRequest('POST', url, formBody, {
            reportProgress: true,
            responseType: 'text',
            withCredentials: true
        });
        return this.http.request(req).pipe(catchError(this.handleError));
        /*return this.http.request<HttpEvent<T>>('POST', url, {body: formBody, headers: headerOptions.headers, withCredentials: true});*/
    }

    getAttachment (url: string): Observable<File> {
        const body = {};
        console.log(`calling ${url}`);
        return this.http.post<File>(url, body, headerOptions).pipe(catchError(this.handleError));
    }

    searchAllRequests(searchField: string, status: string[], stage: string, from: string, quantity: string,
                      order: string, orderField: string, email: string): Observable<Paging<Request>> {
        let statusList = '';
        status.forEach(st => {
            statusList = `${statusList}&status=${st}`;
        });
        let url = `${this.apiUrl}getAll?from=${from}&quantity=${quantity}${statusList}&stage=${stage}`;
        url += `&order=${order}&orderField=${orderField}&email=${encodeURIComponent(email)}&searchField=${searchField}`;

        console.log(`calling ${url}`);
        return this.http.get<Paging<Request>>(url, headerOptions).pipe(
            catchError(this.handleError)
        );
    }

    sendContactFormToService(params: ContactUsMail): Observable<any> {
        const url = `${environment.API_ENDPOINT}/contactUs/sendMail`;
        console.log(`calling ${url}`);
        console.log(`sending ${JSON.stringify(params)}`);

        return this.http.post<any>(url, JSON.stringify(params), headerOptions).pipe(
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
