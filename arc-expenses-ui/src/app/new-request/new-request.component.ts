import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators} from '@angular/forms';
import {
    Attachment, Project, Request, Stage1, Stage2, Stage5a, Stage10, Stage5b, Stage4,
    Stage5, Stage6, Stage7, Stage8, Stage9, Stage3, Stage11, Stage12, Stage13, User
} from '../domain/operation';
import {ManageRequestsService} from '../services/manage-requests.service';
import { ActivatedRoute, Router } from '@angular/router';
import {AuthenticationService} from '../services/authentication.service';
import {DatePipe} from '@angular/common';
import {ManageProjectService} from '../services/manage-project.service';
import {isUndefined} from 'util';
import {HttpEventType, HttpResponse} from '@angular/common/http';
import { requestTypes, supplierSelectionMethods } from '../domain/stageDescriptions';

declare const UIkit: any;

@Component({
    selector: 'app-new-request',
    templateUrl: './new-request.component.html',
    styleUrls: ['./new-request.component.scss']
})
export class NewRequestComponent implements OnInit {

    errorMessage: string;
    showSpinner: boolean;

    requestType: string;
    reqTypes = requestTypes;
    readonly amountLimit = 20000;
    isSupplierRequired = '';

    currentUser: User;

    newRequestForm: FormGroup;

    uploadedFile: File;

    requestedAmount: string;
    searchTerm: string = '';

    request: Request;

    projects: string[] = [];

    chosenProject: Project;

    selMethods = supplierSelectionMethods;

    programSelected = false;

    title = 'Δημιουργία νέου αιτήματος';

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder,
                private requestService: ManageRequestsService,
                private projectService: ManageProjectService,
                private authService: AuthenticationService,
                private router: Router,
                private route: ActivatedRoute) {}


    ngOnInit() {
        this.getUserInfo();
        this.getProjects();
    }

    getUserInfo() {
        this.currentUser = new User();
        this.currentUser.id = this.authService.getUserId();
        this.currentUser.email = this.authService.getUserEmail();
        this.currentUser.firstname = this.authService.getUserFirstName();
        this.currentUser.lastname = this.authService.getUserLastName();
        this.currentUser.firstnameLatin = this.authService.getUserFirstNameInLatin();
        this.currentUser.lastnameLatin = this.authService.getUserLastNameInLatin();
        console.log('this.currentUser is: ', this.currentUser);

        this.requestType = this.route.snapshot.paramMap.get('type');
        this.title = this.reqTypes[this.requestType];
        if (this.requestType !== 'trip') {
            this.isSupplierRequired = '(*)';
        }
        this.createForm();
    }

    getProjects() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.projects = [];
        this.projectService.getAllProjectsNames().subscribe(
            projects => {
                this.projects = projects;
                console.log(this.projects);
            },
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
            },
            () => {
                this.showSpinner = false;
                this.errorMessage = '';
            }
        );
    }

    onChooseRequestType(event: any) {
        if (event.target.value) {
            this.requestType = event.target.value;
            this.title = this.reqTypes[this.requestType];
            this.createForm();
        }
    }

    createForm() {
        this.newRequestForm = this.fb.group({
            name: [''],
            program: ['', Validators.required],
            institute: [''],
            position: ['', Validators.required],
            requestText: ['', Validators.required],
            supplier: [''],
            supplierSelectionMethod: [''],
            amount: ['', [Validators.required, Validators.min(0), Validators.pattern('^\\d+(\\.\\d{1,2})?$')] ],
            sciCoord: ['']
        });
        this.newRequestForm.get('name').setValue(`${this.currentUser.firstname} ${this.currentUser.lastname}`);
        this.newRequestForm.get('name').disable();
    }

    submitRequest() {
        console.log(this.newRequestForm);
        if (this.newRequestForm.valid ) {
            if ( (+this.newRequestForm.get('amount').value > 2500) && isUndefined(this.uploadedFile) ) {
                UIkit.modal.alert('Για αιτήματα άνω των 2.500 € η επισύναψη εγγράφων είναι υποχρεωτική.');
            } else if ( ( this.requestType !== 'trip' ) &&
                        ( (this.newRequestForm.get('supplierSelectionMethod').value !== 'Διαγωνισμός') &&
                          !this.newRequestForm.get('supplier').value )) {

                UIkit.modal.alert('Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.');
            } else if ( (( this.newRequestForm.get('supplierSelectionMethod').value !== 'Απ\' ευθείας ανάθεση' ) &&
                         ( this.requestType !== 'trip' )) &&
                          isUndefined(this.uploadedFile)  ) {

                UIkit.modal.alert('Για αναθέσεις μέσω διαγωνισμού ή έρευνας αγοράς η επισύναψη εγγράφων είναι υποχρεωτική.');
            } else if ( ( +this.newRequestForm.get('amount').value > this.amountLimit) &&
                        ( this.requestType !== 'trip' ) &&
                        ( this.newRequestForm.get('supplierSelectionMethod').value !== 'Διαγωνισμός' ) ) {

                UIkit.modal.alert('Για ποσά άνω των 20.000 € οι αναθέσεις πρέπει να γίνονται μέσω διαγωνισμού.');
            } else {
                this.request = new Request();
                this.request.id = '';
                this.request.type = this.requestType;
                this.request.project = this.chosenProject;
                this.request.requester = this.currentUser;
                this.request.requesterPosition = this.newRequestForm.get('position').value;
                this.request.stage1 = new Stage1();
                /*this.request.stage1.requestDate = this.datePipe.transform(Date.now(), 'dd/MM/yyyy');*/
                this.request.stage1.requestDate = Date.now().toString();
                this.request.stage1.subject = this.newRequestForm.get('requestText').value;
                if (this.requestType !== 'trip') {
                    this.request.stage1.supplier = this.newRequestForm.get('supplier').value;
                    this.request.stage1.supplierSelectionMethod = this.newRequestForm.get('supplierSelectionMethod').value;
                }
                this.request.stage1.amountInEuros = +this.newRequestForm.get('amount').value;
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
                this.request.stage4 = new Stage4();
                // this.request.stage5 = new Stage5();
                this.request.stage5a = new Stage5a();

                if ( (this.request.stage1.amountInEuros > this.amountLimit) ||
                     (this.request.stage1.supplierSelectionMethod === 'Διαγωνισμός') ) {
                    this.request.stage5b = new Stage5b();
                }
                this.request.stage6 = new Stage6();
                this.request.stage7 = new Stage7();
                this.request.stage8 = new Stage8();
                this.request.stage9 = new Stage9();
                this.request.stage10 = new Stage10();
                this.request.stage11 = new Stage11();
                this.request.stage12 = new Stage12();
                this.request.stage13 = new Stage13();

                window.scrollTo(0, 0);
                this.showSpinner = true;
                this.errorMessage = '';
                this.requestService.addRequest(this.request).subscribe (
                    res => {this.request = res; console.log(res); },
                    error => {
                        console.log(error); this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας';
                        this.showSpinner = false;
                    },
                    () => {
                        if (this.uploadedFile) {
                            this.uploadFile();
                        } else {
                            this.showSpinner = false;
                            this.router.navigate(['/requests']);
                        }
                    }
                );
            }

        } else {
            UIkit.modal.alert('Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.');
        }
    }

    uploadFile() {
        this.showSpinner = true;
        this.requestService.uploadAttachment<string>(this.request.archiveId, 'stage1', this.uploadedFile)
            .subscribe(
                event => {
                    // console.log('uploadAttachment responded: ', JSON.stringify(event));
                    if (event.type === HttpEventType.UploadProgress) {
                        console.log('uploadAttachment responded: ', event);
                    } else if ( event instanceof HttpResponse) {
                        console.log('final event:', event.body);
                        this.request.stage1.attachment.url = event.body;
                    }
                },
                error => {
                    console.log(error);
                    this.showSpinner = false;
                },
                () => {
                    console.log('ready to update Request');
                    this.requestService.updateRequest(this.request, this.authService.getUserEmail()).subscribe(
                        res => console.log('updated new request: ', res.status, res.stage, res.stage1),
                        error => {
                            console.log('from update new request', error);
                            this.showSpinner = false;
                        },
                        () => {
                            this.showSpinner = false;
                            this.router.navigate(['/requests']);
                        }
                );
                }
            );
    }

    getProject() {
        this.errorMessage = '';
        if (this.newRequestForm.get('program').value) {
            this.showSpinner = true;

            this.newRequestForm.get('institute').setValue('');
            this.newRequestForm.get('sciCoord').setValue('');

            /*const project = (this.newRequestForm.get('program').value).split('(');
            const institute = project[1].split(')');*/
            this.projectService.getProjectByAcronym(this.newRequestForm.get('program').value).subscribe (
                res => {
                    this.chosenProject = res;
                    console.log(this.chosenProject);
                },
                error => {
                    console.log(error);
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των πληροφοριών για το έργο.';
                    this.showSpinner = false;
                    this.searchTerm = '';
                },
                () => {
                    this.programSelected = true;
                    if (this.chosenProject['institute'] && this.chosenProject.institute.name) {
                        this.newRequestForm.get('institute').setValue(this.chosenProject.institute.name);
                    }
                    if (this.chosenProject && this.chosenProject.scientificCoordinator) {
                        this.newRequestForm.get('sciCoord')
                            .setValue(this.chosenProject.scientificCoordinator.firstname + ' ' + this.chosenProject.scientificCoordinator.lastname);
                    }
                    this.newRequestForm.get('institute').disable();
                    this.newRequestForm.get('sciCoord').disable();
                    this.showSpinner = false;
                    this.searchTerm = '';
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
        console.log(this.newRequestForm.get('program').value);
        this.getProject();
    }

    show (event: any) {
        if (this.newRequestForm.get('program').value && !this.chosenProject) {
            this.getProject();
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    showAmount() {
        this.requestedAmount = this.newRequestForm.get('amount').value.trim();
        this.newRequestForm.get('amount').updateValueAndValidity();
    }

    checkIfTrip() {
        if (this.requestType !== this.route.snapshot.paramMap.get('type')) {
            this.createForm();
        }
        this.requestType = this.route.snapshot.paramMap.get('type');
        this.title = this.reqTypes[this.requestType];
        this.checkIfSupplierIsRequired();

        return (this.requestType !== 'trip');
    }

    checkIfSupplierIsRequired() {
        if ((this.newRequestForm.get('supplierSelectionMethod').value &&
            (this.newRequestForm.get('supplierSelectionMethod').value === 'Διαγωνισμός')) ) {

                this.isSupplierRequired = '';
        } else {
            this.isSupplierRequired = '(*)';
        }
    }

}
