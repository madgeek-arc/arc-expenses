import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpHeaders, HttpRequest } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { catchError } from 'rxjs/operators';
import { Observable } from 'rxjs/Observable';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};



@Injectable()
export class ManageUserService {
    apiUrl = environment.API_ENDPOINT + '/user/';

    constructor(private http: HttpClient) {}

    uploadSignature<T>(email: string, file: File): Observable<HttpEvent<T>> {
        const url = `${this.apiUrl}store/uploadSignatureFile?email=${email}`;
        console.log(`calling ${url}`);

        const formBody: FormData = new FormData();
        formBody.append('file', file, file.name);
        const req = new HttpRequest('POST', url, formBody, {
            reportProgress: true,
            responseType: 'text',
            withCredentials: true
        });
        return this.http.request(req).pipe(catchError(this.handleError));
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
