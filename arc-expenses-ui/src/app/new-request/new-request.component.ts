import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Attachment, Project, Request, Stage1, Stage2, Stage3, Stage5a, Stage5b, Stage4,
         Stage6, User, Vocabulary, RequestApproval } from '../domain/operation';
import { ManageRequestsService } from '../services/manage-requests.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from '../services/authentication.service';
import { DatePipe } from '@angular/common';
import { ManageProjectService } from '../services/manage-project.service';
import { isNullOrUndefined, isUndefined } from 'util';
import { HttpEventType, HttpResponse } from '@angular/common/http';
import { requestTypes, supplierSelectionMethodsMap } from '../domain/stageDescriptions';
import { mergeMap, concatMap, tap } from 'rxjs/operators';

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
    readonly lowAmountLimit = 2500;
    isSupplierRequired = '';

    currentUser: User;

    newRequestForm: FormGroup;

    uploadedFile: File;

    requestedAmount: string;
    showWarning: boolean;
    searchTerm: string = '';

    request: Request;
    requestApproval: RequestApproval;

    projects: Vocabulary[] = [];
    chosenProgramID: string;

    chosenProject: Project;

    selMethods = supplierSelectionMethodsMap;

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
        console.log('in new-request, request type is:', this.route.snapshot.url[this.route.snapshot.url.length - 1].path );
        this.requestType = this.route.snapshot.url[this.route.snapshot.url.length - 1].path;
        this.getUserInfo();
        this.getProjects();
    }

    getUserInfo() {
        this.currentUser = new User();
        this.currentUser.id = this.authService.getUserProp('id');
        this.currentUser.email = this.authService.getUserProp('email');
        this.currentUser.firstname = this.authService.getUserProp('firstname');
        this.currentUser.lastname = this.authService.getUserProp('lastname');
        this.currentUser.firstnameLatin = this.authService.getUserProp('firstnameLatin');
        this.currentUser.lastnameLatin = this.authService.getUserProp('lastnameLatin');
        console.log('this.currentUser is: ', this.currentUser);

        console.log('current type is:', this.requestType);
        this.title = this.reqTypes[this.requestType];
        this.createForm();
        if ( this.requestType === 'regular') {
            this.isSupplierRequired = '(*)';
        }

    }

    getProjects() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.projects = [];
        this.projectService.getAllProjectsNames().subscribe (
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
                if ( isNullOrUndefined(this.projects) || (this.projects.length === 0)) {
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
                }
            }
        );
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

        if (this.newRequestForm.valid ) {
            if ( (+this.newRequestForm.get('amount').value > this.lowAmountLimit) &&
                 (+this.newRequestForm.get('amount').value <= this.amountLimit) &&
                 ( this.newRequestForm.get('supplierSelectionMethod').value === 'direct' ) ) {

                UIkit.modal.alert('Για αιτήματα άνω των 2.500 € η επιλογή προμηθευτή γίνεται μέσω διαγωνισμού ή έρευνας αγοράς.');

            } else if ( ( +this.newRequestForm.get('amount').value > this.amountLimit) && this.checkIfTrip() &&
                        ( this.newRequestForm.get('supplierSelectionMethod').value !== 'competition' ) ) {

                UIkit.modal.alert('Για ποσά άνω των 20.000 € οι αναθέσεις πρέπει να γίνονται μέσω διαγωνισμού.');

            } else if ( this.checkIfTrip() &&
                        ( (this.newRequestForm.get('supplierSelectionMethod').value !== 'competition') &&
                          !this.newRequestForm.get('supplier').value )) {

                UIkit.modal.alert('Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.');

            } else if ( (( this.newRequestForm.get('supplierSelectionMethod').value !== 'direct' ) &&
                           this.checkIfTrip() ) &&
                          isUndefined(this.uploadedFile)  ) {

                UIkit.modal.alert('Για αναθέσεις μέσω διαγωνισμού ή έρευνας αγοράς η επισύναψη εγγράφων είναι υποχρεωτική.');

            } else if ( (+this.newRequestForm.get('amount').value > this.lowAmountLimit) && isUndefined(this.uploadedFile) ) {

                UIkit.modal.alert('Για αιτήματα άνω των 2.500 € η επισύναψη εγγράφων είναι υποχρεωτική.');

            } else {
                this.request = new Request();
                this.request.id = '';
                this.request.type = this.requestType;
                this.request.project = this.chosenProject;
                this.request.user = this.currentUser;
                this.request.requesterPosition = this.newRequestForm.get('position').value;
                this.request.stage1 = new Stage1();
                /*this.request.stage1.requestDate = this.datePipe.transform(Date.now(), 'dd/MM/yyyy');*/
                this.request.stage1.requestDate = Date.now().toString();
                this.request.stage1.subject = this.newRequestForm.get('requestText').value;
                if ( this.checkIfTrip() ) {
                    this.request.stage1.supplier = this.newRequestForm.get('supplier').value;
                    this.request.stage1.supplierSelectionMethod = this.selMethods[this.newRequestForm.get('supplierSelectionMethod').value];
                }
                this.request.stage1.amountInEuros = +this.newRequestForm.get('amount').value;
                if (this.uploadedFile) {
                    this.request.stage1.attachment = new Attachment();
                    this.request.stage1.attachment.filename = this.uploadedFile.name;
                    this.request.stage1.attachment.mimetype = this.uploadedFile.type;
                    this.request.stage1.attachment.size = this.uploadedFile.size;
                    this.request.stage1.attachment.url = '';
                }
                this.request.requestStatus = 'pending';

                this.requestApproval = new RequestApproval();
                this.requestApproval.id = '';
                this.requestApproval.creationDate = Date.now().toString();
                this.requestApproval.stage = '2';
                this.requestApproval.status = 'pending';
                this.requestApproval.stage2 = new Stage2();
                this.requestApproval.stage3 = new Stage3();
                this.requestApproval.stage4 = new Stage4();
                this.requestApproval.stage5a = new Stage5a();

                if ( (this.requestType === 'contract') || (this.requestType === 'services_contract') ||
                     (this.request.stage1.amountInEuros > this.amountLimit) ||
                     (this.request.stage1.supplierSelectionMethod === 'Διαγωνισμός') ) {
                    this.requestApproval.stage5b = new Stage5b();
                }
                this.requestApproval.stage6 = new Stage6();

                window.scrollTo(0, 0);
                this.showSpinner = true;
                this.errorMessage = '';
                this.requestService.addRequest(this.request).subscribe (
                    res => {
                            this.request = res;
                            this.requestApproval.requestId = this.request.id;
                            console.log(res);
                        },
                    error => {
                        console.log(error);
                        UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας.');
                        this.showSpinner = false;
                    },
                    () => {
                        if (this.uploadedFile) {
                            this.uploadFile();
                            // this.uploadAndSubmitRequestAndApproval();
                        } else {
                            this.submitRequestApproval();
                            this.submitRequestAndApproval();
                        }
                    }
                );
            }

        } else {
            UIkit.modal.alert('Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.');
        }
    }

    submitRequestAndApproval() {
        window.scrollTo(0, 0);
        this.showSpinner = true;
        this.errorMessage = '';
        this.requestService.addRequest(this.request).pipe(
                tap (
                    res => {
                        this.request = res;
                        this.requestApproval.requestId = this.request.id;
                        console.log(res);
                    }),
                concatMap (() => this.requestService.addRequestApproval(this.requestApproval)
                )).subscribe (
                    res => this.requestApproval = res,
                    error => {
                        console.log(error);
                        this.showSpinner = false;
                        UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας.');
                    },
                    () => {
                        this.showSpinner = false;
                        this.router.navigate(['/requests']);
                    }
                );
    }

    uploadAndSubmitRequestAndApproval() {
        window.scrollTo(0, 0);
        this.showSpinner = true;
        this.errorMessage = '';
        this.requestService.addRequest(this.request).pipe(
            tap(
                res => {
                    this.request = res;
                    this.requestApproval.requestId = res.id;
                    console.log('addRequest responded:', JSON.stringify(res));
                }
            ),
            concatMap(() => this.requestService.addRequestApproval(this.requestApproval)) ).pipe (
                    tap(res => {
                            this.requestApproval = res;
                            console.log('addRequestApproval responded:', JSON.stringify(this.requestApproval));
                        }
                    ),
                    mergeMap(
                        () => this.requestService.uploadAttachment<string>(this.request.archiveId, 'stage1', this.uploadedFile, 'request')
                    ))
                    .pipe(
                        tap(
                            event => {
                                if (event.type === HttpEventType.UploadProgress) {
                                    console.log('uploadAttachment responded: ', event);
                                } else if ( event instanceof HttpResponse) {
                                    console.log('final event:', event.body);
                                    this.request.stage1.attachment.url = event.body;
                                }
                            }),
                        concatMap(() => this.requestService.updateRequest(this.request, this.authService.getUserProp('email'))
                        ))
                        .subscribe (
                            res => this.request = res,
                            error => {
                                console.log(error);
                                this.showSpinner = false;
                                UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας.');
                            },
                            () => {
                                this.showSpinner = false;
                                this.router.navigate(['/requests']);
                            }
                        );
    }

    submitRequestApproval() {
        this.requestService.addRequestApproval(this.requestApproval).subscribe(
            res => this.requestApproval = res,
            error => {
                console.log(error);
                this.showSpinner = false;
                UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την υποβολή της φόρμας.');
            },
            () => {
                this.showSpinner = false;
                this.router.navigate(['/requests']);
            }
        );
    }

    uploadFile() {
        this.showSpinner = true;
        this.requestService.uploadAttachment<string>(this.request.archiveId, 'stage1', this.uploadedFile, 'request')
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
                    UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την επισύναψη του αρχείου.');
                },
                () => {
                    console.log('ready to update Request');
                    this.requestService.updateRequest(this.request, this.authService.getUserProp('email')).subscribe(
                        res => console.log('updated new request: ', res.requestStatus, res.stage1),
                        error => {
                            console.log('from update new request', error);
                            this.showSpinner = false;
                            UIkit.modal.alert('Παρουσιάστηκε πρόβλημα με την επισύναψη του αρχείου.');
                        },
                        () => {
                            this.submitRequestApproval();
                        }
                    );
                }
            );
    }

    getProject() {
        this.errorMessage = '';
        if (this.newRequestForm.get('program').value) {
            this.chosenProject = new Project();
            this.showSpinner = true;

            this.newRequestForm.get('institute').setValue('');
            this.newRequestForm.get('sciCoord').setValue('');

            this.projectService.getProjectById(this.chosenProgramID).subscribe(
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
                    this.errorMessage = '';
                    if ( !isNullOrUndefined(this.chosenProject) ) {
                        this.programSelected = true;
                        if (this.chosenProject.institute && this.chosenProject.institute.name) {
                            this.newRequestForm.get('institute').setValue(this.chosenProject.institute.name);
                        }
                        if (this.chosenProject && this.chosenProject.scientificCoordinator) {
                            this.newRequestForm.get('sciCoord').setValue(
                                this.chosenProject.scientificCoordinator.firstname + ' ' +
                                      this.chosenProject.scientificCoordinator.lastname);
                        }
                        this.newRequestForm.get('institute').disable();
                        this.newRequestForm.get('sciCoord').disable();
                    } else {
                        this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των πληροφοριών για το έργο.';
                    }
                    this.searchTerm = '';
                    this.showSpinner = false;
                }
            );
        }
    }

    updateSearchTerm(event: any) {
        this.searchTerm = event.target.value;
        if ( (this.searchTerm === '') && !isNullOrUndefined(this.chosenProject) ) {
            this.newRequestForm.get('program').setValue('');
            this.newRequestForm.get('institute').setValue('');
            this.newRequestForm.get('sciCoord').setValue('');
            this.newRequestForm.get('institute').enable();
            this.newRequestForm.get('sciCoord').enable();
        }
    }

    updateProgramInput(project: Vocabulary) {
        /*console.log(this.projects);*/
        this.newRequestForm.get('program').setValue(project.projectAcronym);
        this.chosenProgramID = project.projectID;
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

        if ( !isNullOrUndefined(this.newRequestForm.get('amount').value.trim()) &&
             this.newRequestForm.get('amount').value.trim().includes(',')) {

            const temp = this.newRequestForm.get('amount').value.replace(',', '.');
            this.newRequestForm.get('amount').setValue(temp);
        }

        this.newRequestForm.get('amount').updateValueAndValidity();
        if ( !isNaN(this.newRequestForm.get('amount').value.trim()) ) {
            this.requestedAmount = this.newRequestForm.get('amount').value.trim();
        }

        if ( this.newRequestForm.get('amount').value &&
            (+this.newRequestForm.get('amount').value > this.lowAmountLimit) &&
            (+this.newRequestForm.get('amount').value <= this.amountLimit) &&
            (this.requestType === 'regular') ) {

            this.showWarning = true;
        } else {
            this.showWarning = false;
        }
    }

    checkIfTrip() {
        return ((this.requestType !== 'trip') &&
                (this.requestType !== 'contract'));
    }

    checkIfSupplierIsRequired() {
        if ((this.newRequestForm.get('supplierSelectionMethod').value &&
            (this.newRequestForm.get('supplierSelectionMethod').value === 'competition')) ) {

                this.isSupplierRequired = '';
        } else {
            this.isSupplierRequired = '(*)';
        }
    }


}
