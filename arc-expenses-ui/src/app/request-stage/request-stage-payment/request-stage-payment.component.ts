import { Component, OnInit } from '@angular/core';
import { BaseInfo, Request, RequestApproval, RequestPayment, RequestSummary, Stage5b } from '../../domain/operation';
import { paymentStages, requestTypes, stageIds } from '../../domain/stageDescriptions';
import { RequestInfo } from '../../domain/requestInfoClasses';
import { AnchorItem } from '../../shared/dynamic-loader-anchor-components/anchor-item';
import { ActivatedRoute, Router } from '@angular/router';
import { ManageRequestsService } from '../../services/manage-requests.service';
import { AuthenticationService } from '../../services/authentication.service';
import { mergeMap, tap } from 'rxjs/operators';
import { HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { isNullOrUndefined, isUndefined } from 'util';
import { printRequestPage } from '../print-request-function';

@Component({
    selector: 'request-stage-payment',
    templateUrl: './request-stage-payment.component.html'
})
export class RequestStagePaymentComponent implements OnInit {
    errorMessage: string;
    notFoundMessage: string;
    successMessage: string;
    showSpinner: boolean;

    readonly amountLimit = 20000;
    readonly lowAmountLimit = 2500;

    uploadedFile: File;
    uploadedFileURL: string;

    isSimpleUser: boolean;
    requestId: string;
    currentRequest: Request;
    currentRequestPayment: RequestPayment;
    currentStageName: string;
    canEdit: boolean = false;
    wentBackOneStage: boolean;
    stages: string[];
    stateNames = {
        pending: 'βρίσκεται σε εξέλιξη', under_review: 'βρίσκεται σε εξέλιξη', rejected: 'έχει απορριφθεί', accepted: 'έχει ολοκληρωθεί'
    };
    reqTypes = requestTypes;

    currentRequestInfo: RequestInfo;

    stageLoaderItemList: AnchorItem[];

    updateApprovalToo: boolean;

    constructor(private route: ActivatedRoute,
                private router: Router,
                private requestService: ManageRequestsService,
                private authService: AuthenticationService) {
    }

    ngOnInit() {
        this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');
        console.log(`current user role is: ${this.authService.getUserRole()}`);
        if (this.isSimpleUser) {
            this.router.navigate(['/requests']);
        } else {
            this.getCurrentRequest();
        }
    }

    getCurrentRequest() {
        this.showSpinner = true;
        this.requestId = this.route.snapshot.paramMap.get('id');
        this.errorMessage = '';
        this.notFoundMessage = '';

        /*call api to get request info or throw errorMessage*/
        const result = this.requestService.getRequestPaymentById(this.requestId).pipe(
            tap(res => this.currentRequestPayment = res),
            mergeMap( res =>
                this.requestService.getRequestById(res.requestId, this.authService.getUserProp('email'))
            ));
        result.subscribe(
            req => {
                this.currentRequest = req;
                console.log(`The archiveid of the currentRequest is ${req.archiveId}`);
            },
            error => {
                console.log(error);
                this.showSpinner = false;
                console.log('error status is', error.status);
                if (error instanceof HttpErrorResponse) {
                    if (error.status === 404) {
                        this.notFoundMessage = 'Το αίτημα που ζητήσατε δεν βρέθηκε.';
                    } else {
                        this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
                    }
                }
            },
            () => {
                this.stages = paymentStages;
                this.currentRequestInfo = new RequestInfo(this.currentRequest.id, this.currentRequest.project);
                this.getIfUserCanEditRequest();
            }
        );
    }

    getIfUserCanEditRequest() {
        this.errorMessage = '';
        this.canEdit = null;
        const newBasicInfo = new BaseInfo();
        newBasicInfo.creationDate = this.currentRequestPayment.creationDate;
        newBasicInfo.requestId = this.currentRequestPayment.requestId;
        newBasicInfo.id = this.currentRequestPayment.id;
        newBasicInfo.stage = this.currentRequestPayment.stage;
        newBasicInfo.status = this.currentRequestPayment.status;
        const newSummary = new RequestSummary();
        newSummary.request = this.currentRequest;
        newSummary.baseInfo = newBasicInfo;
        this.requestService.isEditable(newSummary, this.authService.getUserProp('email')).subscribe(
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
                this.successMessage = '';
                this.updateShowStageFields();
            }
        );
    }

    getRequestToGoBack(event: any) {
        this.wentBackOneStage = true;
    }

    getNextStage(stage: string) {
        for (const nextStage of this.currentRequestInfo[stage]['next']) {
            if (!isUndefined(this.currentRequestPayment['stage' + nextStage])) {
                return nextStage;
            }
        }

        return this.currentRequestPayment.stage;
    }

    getPreviousStage(stage: string) {
        for (const prevStage of this.currentRequestInfo[stage]['prev']) {
            if (!isUndefined(this.currentRequestPayment['stage' + prevStage])) {
                return prevStage;
            }
        }

        return this.currentRequestPayment.stage;
    }

    getSubmittedStage(newStage: any) {
        console.log(`got ${JSON.stringify(newStage, null, 1)}`);
        this.currentStageName = 'stage' + this.currentRequestPayment.stage;
        console.log(`submitting as ${this.currentStageName}`);
        this.currentRequestPayment[this.currentStageName] = newStage;

        const submittedStage = this.currentRequestPayment.stage;
        if (this.wentBackOneStage === true) {
            this.currentRequestPayment.stage = this.getPreviousStage(this.currentRequestPayment.stage);
        } else {
            this.currentRequestPayment.stage = this.getNextStage(this.currentRequestPayment.stage);
        }

        // if the pending stage has not changed, the request has been finalized
        if (this.currentRequestPayment.stage === submittedStage) {
            if ( (this.stages.indexOf(this.currentRequestPayment.stage) === (this.stages.length - 1)) &&
                 (newStage['approved'] === true) ) {

                this.currentRequestPayment.status = 'accepted';
                this.currentRequest.requestStatus = 'accepted';
            } else {

                this.currentRequestPayment.status = 'rejected';
                this.currentRequest.requestStatus = 'rejected';
            }
        } else {
            if ( this.wentBackOneStage === true ) {
                this.currentRequestPayment.status = 'under_review';
            } else {
                this.currentRequestPayment.status = 'pending';
            }
        }

        this.wentBackOneStage = false;
        console.log('submitted status:', this.currentRequestPayment.status);

        if ( !isNullOrUndefined(this.uploadedFile) ) {
            this.uploadFile();
        } else {
            this.submitRequestPayment();
        }
    }

    submitRequestPayment() {
        window.scrollTo(0, 0);
        this.showSpinner = true;
        this.errorMessage = '';
        this.successMessage = '';

        if ( !isNullOrUndefined(this.uploadedFile) ) {

            this.currentRequestPayment[this.currentStageName]['attachment']['url'] = this.uploadedFileURL;
            this.uploadedFileURL = '';
            this.uploadedFile = null;
        }
        console.log(`sending ${JSON.stringify(this.currentRequestPayment[this.currentStageName], null, 1)} to updateRequestApproval`);
        /*update this.currentRequest*/
        this.requestService.updateRequestPayment(this.currentRequestPayment).subscribe (
            res => console.log(`update RequestPayment responded: ${res.id}, status=${res.status}, stage=${res.stage}`),
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
            },
            () => {
                this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν.';
                this.showSpinner = false;
                this.getIfUserCanEditRequest();
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
                    console.log('ready to update RequestPayment');
                    this.submitRequestPayment();
                }
            );
    }


    willShowStage(stage: string) {
        const stageField = 'stage' + stage;
        if ( (stage === this.currentRequestPayment.stage) &&
            (this.currentRequestPayment.status !== 'rejected') &&
            (this.currentRequestPayment.status !== 'accepted') &&
            ( (this.authService.getUserRole() === 'ROLE_ADMIN') || (this.canEdit === true) ) ) {

            this.stageLoaderItemList = [
                new AnchorItem(
                    this.currentRequestInfo[stage]['stageComponent'],
                    {
                        currentStage: this.currentRequestPayment['stage' + stage],
                        currentRequestInfo: this.currentRequestInfo
                    }
                ),
            ];

            return 1;

        } else {
            if (stage === '1') {
                return 2;
            }
            if ( !isNullOrUndefined(this.currentRequestPayment[stageField]) &&
                !isNullOrUndefined(this.currentRequestPayment[stageField].date)) {

                if ( !this.isSimpleUser ) {

                    if ( this.stages.indexOf(this.currentRequestPayment.stage) < this.stages.indexOf(stage)) {
                        return 4;
                    }

                    if ( (stage === this.currentRequestPayment.stage) && (this.stages.indexOf(stage) > 0)) {

                        const prevStageField = 'stage' + this.stages[this.stages.indexOf(stage) - 1];
                        if (!isNullOrUndefined(this.currentRequestPayment[prevStageField]) &&
                            !isNullOrUndefined(this.currentRequestPayment[prevStageField].date) &&
                            (this.currentRequestPayment[prevStageField].date > this.currentRequestPayment[stageField].date) ) {

                            return 4;
                        }
                    }

                    if ( (!isUndefined(this.currentRequestPayment[stageField]['approved']) &&
                          this.currentRequestPayment[stageField]['approved'] === true ) ||
                         (stage === '11') ) {

                        return 2;

                    } else {
                        return 3;
                    }
                }
            }
        }

        return 0;
    }

    updateShowStageFields() {
        for ( let i = 0; i < this.stages.length; i++ ) {
            this.currentRequestInfo[this.stages[i]].showStage = this.willShowStage(this.stages[i]);
        }
    }

    linkToFile() {
        if (this.currentRequest.stage1.attachment && this.currentRequest.stage1.attachment.url) {
            /*window.open(this.currentRequest.stage1.attachment.url , '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');*/
            window.open(`${window.location.origin}/arc-expenses-service/request/store/download?requestId=${this.currentRequest.id}&stage=1`,
                '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    printRequest(): void {
        printRequestPage();
    }

    userIsAdmin() {
        return (this.authService.getUserRole() === 'ROLE_ADMIN');
    }

}
