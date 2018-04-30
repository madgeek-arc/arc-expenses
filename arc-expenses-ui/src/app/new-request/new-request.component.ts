import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Attachment, Delegate, Institute, Organization, POY, Project, Request, Requester, Stage1} from '../domain/operation';
import {ManageRequestsService} from '../services/manage-requests.service';
import {Router} from '@angular/router';
import {AuthenticationService} from '../services/authentication.service';
import {DatePipe} from '@angular/common';
import {ManageProjectService} from '../services/manage-project.service';

@Component({
    selector: 'app-new-request',
    templateUrl: './new-request.component.html',
    styleUrls: ['./new-request.component.scss']
})
export class NewRequestComponent implements OnInit {

    errorMessage: string;

    currentUser: Requester;

    newRequestForm: FormGroup;

    uploadedFile: File;

    request: Request;

    institutes = ['ILSP', 'IMSI', 'ISI', 'SPU', 'PPA'];

    programs = ['program1', 'program2', 'program3'];

    /*projects: Project[] = [];*/
    projects = [
        {
            id: '4',
            name: 'program1',
            acronym: 'PROG1',
            institute: {
                id: 'INST1',
                name: 'institute 1',
                organization: null,
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: null,
                accountingPayment: null,
                accountingDirector: null,
                diaugeia: null,
            },
            parentProject: null,
            scientificCoordinator: null,
            operator: [],
            startDate: '',
            endDate: ''
        },
        {
            id: '5',
            name: 'program2',
            acronym: 'PROG2',
            institute: {
                id: 'INST2',
                name: 'institute 2',
                organization: null,
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: null,
                accountingPayment: null,
                accountingDirector: null,
                diaugeia: null,
            },
            parentProject: null,
            scientificCoordinator: null,
            operator: [],
            startDate: '',
            endDate: ''
        },
        {
            id: '6',
            name: 'program3',
            acronym: 'PROG3',
            institute: {
                id: 'INST3',
                name: 'institute 3',
                organization: null,
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: null,
                accountingPayment: null,
                accountingDirector: null,
                diaugeia: null,
            },
            parentProject: null,
            scientificCoordinator: null,
            operator: [],
            startDate: '',
            endDate: ''
        },
    ];
    chosenProject: Project;

    selMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];

    programSelected = false;

    title = 'Πρωτογενές Αίτημα & Έγκριση Δαπάνης';

    datePipe = new DatePipe('en-us');

    constructor(private fb: FormBuilder,
                private requestService: ManageRequestsService,
                private projectService: ManageProjectService,
                private authService: AuthenticationService,
                private router: Router) {
    }


    ngOnInit() {
        this.getUserInfo();
        this.getProjects();
        this.createForm();
    }

    getUserInfo() {
        this.currentUser = new Requester();
        this.currentUser.id = this.authService.getUserId();
        this.currentUser.firstname = this.authService.getUserFirstName();
        this.currentUser.lastname = this.authService.getUserLastName();
        this.currentUser.firstnameLatin = this.authService.getUserFirstNameInLatin();
        this.currentUser.lastnameLatin = this.authService.getUserLastNameInLatin();
        console.log('this.currentUser is: ', this.currentUser);
    }

    getProjects() {
        this.projectService.getAllProjects().subscribe(
            /*projects => this.projects = projects,*/
            projects => console.log(projects),
            error => console.log(error)
        );
    }

    createForm() {
        this.newRequestForm = this.fb.group({
            name: ['', [Validators.required, Validators.pattern('^\\w+\\s\\w+$')] ],
            program: ['', Validators.required],
            institute: ['', Validators.required],
            position: ['', Validators.required],
            requestText: ['', Validators.required],
            supplier: ['', Validators.required],
            supplierSelectionMethod: ['', Validators.required],
            ammount: ['', [Validators.required, Validators.pattern('^\\d*(\\.\\d{1,2})?$')] ],
            director: ['', Validators.required]
        });
        this.newRequestForm.get('name').setValue(`${this.currentUser.firstname} ${this.currentUser.lastname}`);
        this.newRequestForm.get('name').disable();
        this.newRequestForm.get('institute').disable();
        this.newRequestForm.get('director').disable();
    }

    submitRequest() {
        if (this.newRequestForm.valid) {

            this.request = new Request();
            /*this.request.id = '';*/
            this.request.project = this.chosenProject;
            this.request.requester = this.currentUser;
            this.request.requesterPosition = this.newRequestForm.get('position').value;
            this.request.stage1 = new Stage1();
            this.request.stage1.requestDate = this.datePipe.transform(Date.now(), 'dd/MM/yyyy');
            this.request.stage1.subject = this.newRequestForm.get('requestText').value;
            this.request.stage1.supplier = this.newRequestForm.get('supplier').value;
            this.request.stage1.supplierSelectionMethod = this.newRequestForm.get('supplierSelectionMethod').value;
            this.request.stage1.amountInEuros = this.newRequestForm.get('ammount').value;
            if (this.uploadedFile) {
                this.request.stage1.attachment = new Attachment();
                this.request.stage1.attachment.filename = this.uploadedFile.name;
                this.request.stage1.attachment.mimetype = this.uploadedFile.type;
                this.request.stage1.attachment.size = this.uploadedFile.size;
                /*this.request.stage1.attachment.url='';*/
            }
            this.request.stage = '2';

            this.requestService.addRequest(this.request).subscribe(
                res => {this.request = res; console.log(res); },
                error => {console.log(error); this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας'; },
                () => this.router.navigate(['/requests'])
            );
        } else {
            this.errorMessage = 'Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά';
        }
    }

    getProject() {
        if (this.newRequestForm.get('program').value) {
            this.projectService.getProjectByAcronym(this.newRequestForm.get('program').value).subscribe(
                res => this.chosenProject = res,
                error => console.log(error)
            );
        }
    }

    show(event: any) {
        if (this.newRequestForm.get('program').value) {
            this.programSelected = true;
            this.chosenProject = this.projects[this.newRequestForm.get('program').value];
            console.log(this.chosenProject);
            this.newRequestForm.get('institute').setValue(this.chosenProject['institute'].name);
            this.newRequestForm.get('director')
                .setValue(this.chosenProject.institute.director.firstname + ' ' + this.chosenProject.institute.director.lastname);
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

}
