/**
 * Created by stefania on 7/17/17.
 */
import { Injectable } from '@angular/core';
import { PageContent } from '../../domain/page-content';
import { Observable } from 'rxjs/Observable';
import { tap } from 'rxjs/operators';
import { HttpClient, HttpResponse } from '@angular/common/http';


@Injectable()
export class HelpContentService {

    // private _helpServiceUrl = process.env.FAQ_ENDPOINT;
    private _helpServiceUrl = '';

    constructor (private http: HttpClient) {
    }

    cache: any = {};

    getActivePageContent(route: string): Observable<PageContent> {
        if (!this.cache[route]) {
            this.cache[route] = this.http.get(this._helpServiceUrl + '/page/route?q=' + route)
                .pipe(tap(
                    res => res,
                    error => this.handleError(error)
                ));
        }
        return this.cache[route];
    }

    private extractData(res: HttpResponse<any>) {
        const body = res;
        return body.body || { };
    }

    private handleError (error: HttpResponse<any> | any) {
        // In a real world app, we might use a remote logging infrastructure
        // We'd also dig deeper into the error to get a better message
        let errMsg = '';
        console.log(error);
        if (error instanceof HttpResponse) {
            const body = error.statusText || '';
            // const err = body.error || JSON.stringify(body);
            errMsg = `${error.status} - ${error.statusText || ''} ${body}`;
        } else {
            errMsg = (error.statusText) ? error.statusText :
                error.status ? `${error.status} - ${error.statusText}` : 'Server error';
            console.error(errMsg); // log to console instead
        }
        return Observable.throw(errMsg);
    }
}
