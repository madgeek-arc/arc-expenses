import { Component, OnInit } from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {
    Attachment, Delegate, Institute, Organization, POY, Project, Request, Requester, Stage1, Stage2, Stage3a, Stage10, Stage3b, Stage4,
    Stage5, Stage6,
    Stage7,
    Stage8,
    Stage9, Stage3
} from '../domain/operation';
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

    requestedAmmount: string;
    searchTerm: string = '';

    request: Request;

    institutes = ['ILSP', 'IMSI', 'ISI', 'SPU', 'PPA'];

    programs = ['program1', 'program2', 'program3'];

    projects: string[] = [];
/*    projects = [
        {
            id: '4',
            name: 'program1',
            acronym: 'PROG1',
            institute: {
                id: 'INST1',
                name: 'institute 1',
                organization: {
                    id: 'org1',
                    name: 'Organization 1',
                    POY: {
                        email: 'POY 1',
                        firstname: 'poyfn',
                        lastname: 'poyln',
                        delegates: []
                    },
                    director: {
                        email: 'director 1',
                        firstname: 'dirfn',
                        lastname: 'dirln',
                        delegates: []
                    },
                    dioikitikoSumvoulio: {
                        email: 'DS 1',
                        firstname: 'dsfn',
                        lastname: 'dsln',
                        delegates: []
                    }
                },
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: {
                    email: 'accounting 1',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingPayment: {
                    email: 'accountingPayment 1',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingDirector: {
                    email: 'accountingDir 1',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                diaugeia: {
                    email: 'diaugeia 1',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
            },
            scientificCoordinator: {
                email: 'scientificCoord 1',
                firstname: 'poyfn',
                lastname: 'poyln',
                delegates: []
            },
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
                organization: {
                    id: 'org1',
                    name: 'Organization 2',
                    POY: {
                        email: 'POY 2',
                        firstname: 'poyfn',
                        lastname: 'poyln',
                        delegates: []
                    },
                    director: {
                        email: 'director 2',
                        firstname: 'dirfn',
                        lastname: 'dirln',
                        delegates: []
                    },
                    dioikitikoSumvoulio: {
                        email: 'DS 2',
                        firstname: 'dsfn',
                        lastname: 'dsln',
                        delegates: []
                    }
                },
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: {
                    email: 'accounting 2',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingPayment: {
                    email: 'accountingPayment 2',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingDirector: {
                    email: 'accountingDir 2',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                diaugeia: {
                    email: 'diaugeia 2',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
            },
            scientificCoordinator: {
                email: 'scientificCoord 2',
                firstname: 'poyfn',
                lastname: 'poyln',
                delegates: []
            },
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
                organization: {
                    id: 'org1',
                    name: 'Organization 3',
                    POY: {
                        email: 'POY 3',
                        firstname: 'poyfn',
                        lastname: 'poyln',
                        delegates: []
                    },
                    director: {
                        email: 'director 3',
                        firstname: 'dirfn',
                        lastname: 'dirln',
                        delegates: []
                    },
                    dioikitikoSumvoulio: {
                        email: 'DS 3',
                        firstname: 'dsfn',
                        lastname: 'dsln',
                        delegates: []
                    }
                },
                director: {
                    email: 'directorEmail@email.com',
                    firstname: 'dirFirstName',
                    lastname: 'dirLastName',
                    delegates: [],
                },
                accountingRegistration: {
                    email: 'accounting 3',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingPayment: {
                    email: 'accountingPayment 3',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                accountingDirector: {
                    email: 'accountingDir 3',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
                diaugeia: {
                    email: 'diaugeia 3',
                    firstname: 'poyfn',
                    lastname: 'poyln',
                    delegates: []
                },
            },
            scientificCoordinator: {
                email: 'scientificCoord 3',
                firstname: 'poyfn',
                lastname: 'poyln',
                delegates: []
            },
            operator: [],
            startDate: '',
            endDate: ''
        },
    ];*/

    chosenProject: Project;

    selMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];

    programSelected = false;

    title = 'Πρωτογενές Αίτημα & Έγκριση Δαπάνης';

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder,
                private requestService: ManageRequestsService,
                private projectService: ManageProjectService,
                private authService: AuthenticationService,
                private router: Router) {}


    ngOnInit() {
        this.getUserInfo();
        this.getProjects();
        this.createForm();
    }

    getUserInfo() {
        this.currentUser = new Requester();
        this.currentUser.id = this.authService.getUserId();
        this.currentUser.email = this.authService.getUserEmail();
        this.currentUser.firstname = this.authService.getUserFirstName();
        this.currentUser.lastname = this.authService.getUserLastName();
        this.currentUser.firstnameLatin = this.authService.getUserFirstNameInLatin();
        this.currentUser.lastnameLatin = this.authService.getUserLastNameInLatin();
        /*this.currentUser.id = 'userid';
        this.currentUser.email = 'user@gmail.com';
        this.currentUser.firstname = 'firstname';
        this.currentUser.lastname = 'lastname';
        this.currentUser.firstnameLatin = 'firstnameinlatin';
        this.currentUser.lastnameLatin = 'lastnameinlatin';*/
        console.log('this.currentUser is: ', this.currentUser);
    }

    getProjects() {
        this.projectService.getAllProjectsNames().subscribe(
            projects => {
                this.projects = projects;
                console.log(this.projects);
            },
            error => console.log(error)
        );
        /*this.projects = ['proj1', 'proj2', 'proj3'];*/
    }

    createForm() {
        this.newRequestForm = this.fb.group({
            name: [''],
            program: ['', Validators.required],
            institute: [''],
            position: ['', Validators.required],
            requestText: ['', Validators.required],
            supplier: ['', Validators.required],
            supplierSelectionMethod: ['', Validators.required],
            ammount: ['', [Validators.required, Validators.min(0), Validators.pattern('^\\d+(\\.\\d{1,2})?$')] ],
            director: ['']
        });
        this.newRequestForm.get('name').setValue(`${this.currentUser.firstname} ${this.currentUser.lastname}`);
        this.newRequestForm.get('name').disable();
        this.newRequestForm.get('institute').disable();
        this.newRequestForm.get('director').disable();
    }

    submitRequest() {
        console.log(this.newRequestForm);
        if (this.newRequestForm.valid) {
            this.request = new Request();
            this.request.id = '';
            this.request.project = this.chosenProject;
            this.request.requester = this.currentUser;
            this.request.requesterPosition = this.newRequestForm.get('position').value;
            this.request.stage1 = new Stage1();
            this.request.stage1.requestDate = this.datePipe.transform(Date.now(), 'dd/MM/yyyy');
            this.request.stage1.subject = this.newRequestForm.get('requestText').value;
            this.request.stage1.supplier = this.newRequestForm.get('supplier').value;
            this.request.stage1.supplierSelectionMethod = this.newRequestForm.get('supplierSelectionMethod').value;
            this.request.stage1.amountInEuros = +this.newRequestForm.get('ammount').value;
            if (this.uploadedFile) {
                this.request.stage1.attachment = new Attachment();
                this.request.stage1.attachment.filename = this.uploadedFile.name;
                this.request.stage1.attachment.mimetype = this.uploadedFile.type;
                this.request.stage1.attachment.size = this.uploadedFile.size;
                this.request.stage1.attachment.url = '';
            }
            this.request.stage = '2';
            this.request.status = 'pending';
            this.request.stage2 = new Stage2();
            this.request.stage3 = new Stage3();
            this.request.stage3a = new Stage3a();
            this.request.stage3b = new Stage3b();
            this.request.stage4 = new Stage4();
            this.request.stage5 = new Stage5();
            this.request.stage6 = new Stage6();
            this.request.stage7 = new Stage7();
            this.request.stage8 = new Stage8();
            this.request.stage9 = new Stage9();
            this.request.stage10 = new Stage10();

            this.requestService.addRequest(this.request).subscribe (
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
            this.errorMessage = '';
            this.projectService.getProjectByAcronym(this.newRequestForm.get('program').value).subscribe (
                res => {
                    this.chosenProject = res;
                    console.log(this.chosenProject);
                },
                error => {
                    console.log(error);
                },
                () => {
                    this.programSelected = true;
                    this.newRequestForm.get('institute').setValue(this.chosenProject['institute'].name);
                    this.newRequestForm.get('director')
                        .setValue(this.chosenProject.institute.director.firstname + ' ' + this.chosenProject.institute.director.lastname);
                }
            );
        }
    }

    updateSearchTerm(event: any) {
        this.searchTerm = event.target.value;
    }

    updateProgramInput(acronym: string) {
        console.log(this.projects);
        this.newRequestForm.get('program').setValue(acronym);
        this.searchTerm = '';
        console.log(this.newRequestForm.get('program').value);
        this.getProject();
    }

    show (event: any) {
        console.log('searching for prog');
        if (this.newRequestForm.get('program').value && this.chosenProject) {
            this.getProject();
            this.programSelected = true;
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    showAmmount() {
        /*this.requestedAmmount = '';
        for (let c of this.newRequestForm.get('ammount').value.trim()) {
            if (c === '.') {
                this.requestedAmmount += ',';
            } else if (c === ',') {
                this.requestedAmmount += '.';
            } else {
                this.requestedAmmount += c;
            }
        }*/
        this.requestedAmmount = this.newRequestForm.get('ammount').value.trim();
        this.newRequestForm.get('ammount').updateValueAndValidity();
    }

}

export function ammountValidator(f: AbstractControl) {
    console.log('checking ammount');
    const inputAmmount: string = f.get('ammount').value;
    let countNumbers: number = 0;
    let startCounting: boolean;
    let addedDecimals: boolean;

    if (inputAmmount) {
        console.log(`got ${inputAmmount}`);
        if (isNaN(+inputAmmount[0])) {
            return 'invalid';
        }
        for (let c of inputAmmount) {
            if (isNaN(+c) && c !== '.' && c !== ',') {
                return 'invalid';
            }
            if (c === '.') {
                if (addedDecimals) {
                    return 'invalid';
                }
                if (startCounting) {
                    if (!countNumbers) {
                        return 'invalid';
                    }
                } else {
                    startCounting = true;
                }
            } else if (c === ',') {
                addedDecimals = true;
                if (startCounting && countNumbers < 3) {
                    return 'invalid';
                }
            } else {
                if (startCounting) {
                    countNumbers++;
                    if (countNumbers === 3) {
                        startCounting = false;
                        countNumbers = 0;
                    }
                }
            }
        }
        return null;
    } else {
        return 'invalid';
    }
}
