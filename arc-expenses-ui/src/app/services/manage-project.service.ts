import {Injectable} from '@angular/core';
import { Project, Vocabulary } from '../domain/operation';
import {catchError} from 'rxjs/operators';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';
import {environment} from '../../environments/environment';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};

@Injectable()
export class ManageProjectService {

    apiUrl = environment.API_ENDPOINT + '/project/';

    constructor(private http: HttpClient) {}


    addProject(newProject: Project): Observable<Project> {
        const url = `${this.apiUrl}add`;
        console.log(`calling ${url}`);
        return this.http.post<Project>(url, newProject, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    getAllProjects(): Observable<Project[]> {
        const url = `${this.apiUrl}getAll`;
        console.log(`calling ${url}`);
        return this.http.get<Project[]>(url, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }


    getAllProjectsNames(): Observable<Vocabulary[]> {
        const url = `${this.apiUrl}getAllProjectNames`;
        console.log(`calling ${url}`);
        return this.http.get<Vocabulary[]>(url, headerOptions)
            .pipe(
                catchError(this.handleError)
            );
    }

    getProjectById(projectId: string): Observable<Project> {
        const url = `${this.apiUrl}getById/${projectId}`;
        console.log(`calling ${url}`);
        return this.http.get<Project>(url, headerOptions)
            .pipe(
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
