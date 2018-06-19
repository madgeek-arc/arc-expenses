import { Component, OnInit } from '@angular/core';
import { Attachment, Request, Stage5b } from '../domain/operation';
import { ActivatedRoute, Router } from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { isNull, isNullOrUndefined, isUndefined } from 'util';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {HttpEventType, HttpResponse} from '@angular/common/http';
import { requestTypes, stageIds, stagesDescriptionMap, supplierSelectionMethods } from '../domain/stageDescriptions';
import { printRequestPage } from './print-request-function';
import { noUndefined } from '@angular/compiler/src/util';

@Component({
    selector: 'app-request-stage',
    templateUrl: './request-stage.component.html',
    styleUrls: ['./request-stage.component.scss']
})
export class RequestStageComponent implements OnInit {
    errorMessage: string;
    successMessage: string;
    showSpinner: boolean;

    uploadedFile: File;
    uploadedFileURL: string;

    isSimpleUser: boolean;
    requestId: string;
    currentRequest: Request;
    currentStageName: string;
    canEdit: boolean = false;
    wentBackOneStage: boolean;
    stages = stageIds;
    stagesMap = stagesDescriptionMap;
    stateNames = {pending: 'βρίσκεται σε εξέλιξη', rejected: 'έχει απορριφθεί', accepted: 'έχει ολοκληρωθεί'};
    reqTypes = requestTypes;

    sendInfoToStage5b: string[];

    constructor(private route: ActivatedRoute,
                private router: Router,
                private requestService: ManageRequestsService,
                private authService: AuthenticationService) {
    }

    ngOnInit() {
        this.getCurrentRequest();
        this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');
        console.log(`current user role is: ${this.authService.getUserRole()}`);
    }

    getCurrentRequest() {
        this.showSpinner = true;
        this.requestId = this.route.snapshot.paramMap.get('id');
        this.errorMessage = '';

        /*call api to get request info or throw errorMessage*/
        this.requestService.getRequestById(this.requestId, this.authService.getUserEmail()).subscribe(
            res => {
                this.currentRequest = res;
                console.log(`The archiveid of the currentRequest is ${res.archiveId}`);
            },
            error => {
                console.log(error);
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
                this.showSpinner = false;
            },
            () => {
                if ( isNullOrUndefined(this.currentRequest) ) {
                    this.showSpinner = false;
                    this.errorMessage = 'Το αίτημα που ζητήσατε δεν βρέθηκε.';
                } else {
                    this.checkIfStageIs5b();
                    this.getIfUserCanEditRequest();
                }
            }
        );
    }

    checkIfStageIs5b() {
        if ( (this.currentRequest.stage === '5b') &&
            (this.currentRequest.stage1.supplierSelectionMethod === 'Διαγωνισμός') ) {
            this.sendInfoToStage5b = [];
            this.sendInfoToStage5b.push('');
            this.sendInfoToStage5b.push('');
            if ( !isNullOrUndefined(this.currentRequest.stage1.supplier) ) {
                this.sendInfoToStage5b[0] = this.currentRequest.stage1.supplier;
            }
            if ( !isNullOrUndefined(this.currentRequest.stage1.amountInEuros) ) {
                this.sendInfoToStage5b[1] = (this.currentRequest.stage1.amountInEuros).toString();
            }
        }
    }

    getIfUserCanEditRequest() {
        this.errorMessage = '';
        this.requestService.isEditable(this.currentRequest, this.authService.getUserEmail()).subscribe(
            res => this.canEdit = res,
            error => {
                console.log(error);
                this.canEdit = false;
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
            },
            () => {
                this.showSpinner = false;
                console.log('this.canEdit is ', this.canEdit);
            }
        );
    }

    getRequestToGoBack(event: any) {
        this.wentBackOneStage = true;
    }

    getNextStage(stage: string) {
        for ( const nextStage of this.stagesMap[stage]['next'] ) {
            if ( !isUndefined(this.currentRequest['stage' + nextStage]) ) {
                return nextStage;
            }
        }

        return this.currentRequest.stage;
    }

    getPreviousStage(stage: string) {
        for ( const prevStage of this.stagesMap[stage]['prev'] ) {
            if ( !isUndefined(this.currentRequest['stage' + prevStage]) ) {
                return prevStage;
            }
        }

        return this.currentRequest.stage;
    }

    getSubmittedStage(newStage: any) {
        console.log(`got ${JSON.stringify(newStage, null, 1)}`);
        this.currentStageName = 'stage' + this.currentRequest.stage;
        console.log(`submitting as ${this.currentStageName}`);
        this.currentRequest[this.currentStageName] = newStage;
        if (newStage['approved']) {
            if (this.currentRequest.stage === '13') {
                this.currentRequest.status = 'accepted';
            } else {
                this.currentRequest.status = 'pending';
            }
        } else if (this.currentRequest.stage === '6' || this.currentRequest.stage === '11' || (this.wentBackOneStage === true)) {
            this.currentRequest.status = 'pending';
        } else {
            this.currentRequest.status = 'rejected';
        }
        if (this.wentBackOneStage === true) {
            this.currentRequest.stage = this.getPreviousStage(this.currentRequest.stage);
        } else {
            this.currentRequest.stage = this.getNextStage(this.currentRequest.stage);
        }
        this.checkIfStageIs5b();
        this.wentBackOneStage = false;
        console.log('submitted status:', this.currentRequest.status);

        if ( !isNullOrUndefined(this.uploadedFile) ) {
            this.uploadFile();
        } else {
            this.submitRequest();
        }
    }

    getUpdatedRequest(updatedRequest: Request) {
        this.currentRequest = updatedRequest;
        this.currentRequest.stage = '2';
        if ( !isNullOrUndefined(this.uploadedFile) ) {
            this.uploadFile();
        } else {
            this.submitRequest();
        }
    }

    submitRequest() {
        window.scrollTo(0, 0);
        this.showSpinner = true;
        this.errorMessage = '';
        this.successMessage = '';

        if ( !isNullOrUndefined(this.uploadedFile) ) {

            this.currentRequest[this.currentStageName]['attachment']['url'] = this.uploadedFileURL;
            this.uploadedFileURL = '';
            this.uploadedFile = null;
        }
        console.log(`sending ${JSON.stringify(this.currentRequest[this.currentStageName], null, 1)} to updateRequest`);
        /*update this.currentRequest*/
        this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe (
            res => console.log(`update Request responded: ${res.id}, status=${res.status}, stage=${res.stage}`),
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
            },
            () => {
                this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν.';
                this.showSpinner = false;
                /*this.router.navigate(['requests/request-stage', this.currentRequest.id]);*/
                window.location.href = '/requests/request-stage/' + this.currentRequest.id;
            }
        );
    }

    uploadFile() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.requestService.uploadAttachment<string>(this.currentRequest.archiveId, this.currentStageName, this.uploadedFile)
            .subscribe(
                event => {
                    // console.log('uploadAttachment responded: ', JSON.stringify(event));
                    if (event.type === HttpEventType.UploadProgress) {
                        console.log('uploadAttachment responded: ', event);
                    } else if ( event instanceof HttpResponse) {
                        console.log('final event:', event.body);
                        this.uploadedFileURL = event.body;
                    }
                },
                error => {
                    console.log(error);
                    this.uploadedFile = null;
                    this.showSpinner = false;
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
                },
                () => {
                    console.log('ready to update Request');
                    // this.showSpinner = false;
                    this.submitRequest();
                }
            );
    }


    willShowStage(stage: string) {
        const stageField = 'stage' + stage;
        if ( (stage === this.currentRequest.stage) &&
            (this.currentRequest.status !== 'rejected') &&
            (this.currentRequest.status !== 'accepted') &&
            ( (this.authService.getUserRole() === 'ROLE_ADMIN') || (this.canEdit === true) ) ) {

            return 1;

        } else {
            if ( !isNullOrUndefined(this.currentRequest[stageField]) && !isNullOrUndefined(this.currentRequest[stageField].date)) {

                if (!this.isSimpleUser || (stage === '2')) {

                    if ( this.stages.indexOf(this.currentRequest.stage) < this.stages.indexOf(stage)) {
                        return 4;
                    }

                    if ((!isUndefined(this.currentRequest[stageField]['approved']) &&
                            this.currentRequest[stageField]['approved'] === true) ||
                        (stage === '6') || (stage === '11')) {

                        return 2;

                    } else {
                        return 3;
                    }
                }
            }
        }

        return 0;
    }

    linkToFile() {
        if (this.currentRequest.stage1.attachment && this.currentRequest.stage1.attachment.url) {
            window.open(this.currentRequest.stage1.attachment.url, '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
        }
    }

    getNewSupplierAndAmount(newVals: string[]) {
        if (newVals && newVals.length === 2) {
            this.currentRequest.stage1.supplier = newVals[0];
            this.currentRequest.stage1.amountInEuros = +newVals[1];
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    printRequest(): void {
        printRequestPage();
    }

}
