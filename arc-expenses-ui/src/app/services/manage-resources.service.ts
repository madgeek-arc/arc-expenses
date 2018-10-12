import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {Delegate, Institute, Organization, POI} from '../domain/operation';
import {Observable} from 'rxjs/Observable';

const headerOptions = {
    headers : new HttpHeaders().set('Content-Type', 'application/json').set('Accept', 'application/json'),
    withCredentials: true
};


@Injectable()
export class ManageResourcesService {

    apiUrl = environment.API_ENDPOINT;

    constructor(private http: HttpClient) {}

    /* Institutes */
    getAllInstitutes(): Observable<string[]> {
        const url = `${this.apiUrl}/institute/getAll`;
        console.log(`calling ${url}`);
        return this.http.get<string[]>(url, headerOptions);
    }

    getInstituteNames(): Observable<Map<string,string>> {
        const url = `${this.apiUrl}/institute/getInstituteNames`;
        console.log(`calling ${url}`);
        return this.http.get<Map<string,string>>(url, headerOptions);
    }

    getInstituteById(id: string): Observable<Institute> {
        const url = `${this.apiUrl}/institute/getById/${id}`;
        console.log(`calling ${url}`);
        return this.http.get<Institute>(url, headerOptions);
    }


    addInstitute(newInstitute: Institute): Observable<Institute> {
        const url = `${this.apiUrl}/institute/add`;
        console.log(`calling ${url}`);
        return this.http.post<Institute>(url, newInstitute, headerOptions);
    }


    /* Organizations */
    getAllOrganizations(): Observable<string[]> {
        const url = `${this.apiUrl}/organization/getAll`;
        console.log(`calling ${url}`);
        return this.http.get<string[]>(url, headerOptions);
    }

    getOrganizationById(id: string): Observable<Organization> {
        const url = `${this.apiUrl}/organization/getById/${id}`;
        console.log(`calling ${url}`);
        return this.http.get<Organization>(url, headerOptions);
    }

    addOrganization(newOrganization: Organization): Observable<Organization> {
        const url = `${this.apiUrl}/organization/add`;
        console.log(`calling ${url}`);
        return this.http.post<Organization>(url, newOrganization, headerOptions);
    }

    /* POIs */
    getAllPOIs(): Observable<string[]> {
        const url = `${this.apiUrl}/poi/getAll`;
        console.log(`calling ${url}`);
        return this.http.get<string[]>(url, headerOptions);
    }

    getPOIById(id: string): Observable<POI> {
        const url = `${this.apiUrl}/poi/getById/${id}`;
        console.log(`calling ${url}`);
        return this.http.get<POI>(url, headerOptions);
    }

    addPOI(newPOI: POI): Observable<POI> {
        const url = `${this.apiUrl}/poi/add`;
        console.log(`calling ${url}`);
        return this.http.post<POI>(url, newPOI, headerOptions);
    }


    /* Delegates */
    getAllDelegates(): Observable<string[]> {
        const url = `${this.apiUrl}/delegate/getAll`;
        console.log(`calling ${url}`);
        return this.http.get<string[]>(url, headerOptions);
    }

    getDelegateById(id: string): Observable<Delegate> {
        const url = `${this.apiUrl}/delegate/getById/${id}`;
        console.log(`calling ${url}`);
        return this.http.get<Delegate>(url, headerOptions);
    }

    addDelegate(newDelegate: Delegate): Observable<Delegate> {
        const url = `${this.apiUrl}/delegate/add`;
        console.log(`calling ${url}`);
        return this.http.post<Delegate>(url, newDelegate, headerOptions);
    }

}
