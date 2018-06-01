import { Component, OnInit } from '@angular/core';
import { Attachment, Request, Stage5b } from '../domain/operation';
import { ActivatedRoute } from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { isNullOrUndefined, isUndefined } from 'util';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
    selector: 'app-request-stage',
    templateUrl: './request-stage.component.html',
    styleUrls: ['./request-stage.component.scss']
})
export class RequestStageComponent implements OnInit {
    errorMessage: string;
    successMessage: string;
    showSpinner: boolean;

    updateStage1Form: FormGroup;
    uploadedFile: File;
    requestedAmount: string;
    stage1AttachmentName = '';
    readonly amountLimit = 20000;

    isSimpleUser: boolean;
    requestId: string;
    currentRequest: Request;
    canEdit: boolean = false;
    wentBackOneStage: boolean;
    stages = ['1', '2', '3', '4', '5', '5a', '5b', '6', '7', '8', '9', '10', '11', '12', '13'];
    stateNames = {pending: 'βρίσκεται σε εξέλιξη', rejected: 'έχει απορριφθεί', accepted: 'έχει ολοκληρωθεί'};
    reqTypes = {regular: 'Πρωτογενές Αίτημα', trip: 'Ταξίδι', contract: 'Σύμβαση'};
    selMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];

    constructor(private fb: FormBuilder, private route: ActivatedRoute,
                private requestService: ManageRequestsService,
                private authService: AuthenticationService) {
    }

    ngOnInit() {
        this.getCurrentRequest();
        this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');
        console.log(`current user role is: ${this.authService.getUserRole()}`);
    }

    createForm() {
        this.updateStage1Form = this.fb.group({
            requestText: [''],
            supplier: [''],
            supplierSelectionMethod: [''],
            amount: ['', [Validators.min(0), Validators.pattern(/^\\d+(\\.\\d{1,2})?$/)]]
        });
        this.updateStage1Form.get('requestText').setValue(this.currentRequest.stage1.subject);
        this.updateStage1Form.get('supplier').setValue(this.currentRequest.stage1.supplier);
        this.updateStage1Form.get('supplierSelectionMethod').setValue(this.currentRequest.stage1.supplierSelectionMethod);
        this.updateStage1Form.get('amount').setValue(this.currentRequest.stage1.amountInEuros);
        if (this.currentRequest.stage1.attachment) {
            this.stage1AttachmentName = this.currentRequest.stage1.attachment.filename;
        }
    }

    getCurrentRequest() {
        this.showSpinner = true;
        this.requestId = this.route.snapshot.paramMap.get('id');
        this.errorMessage = '';

        /*call api to get request info or throw errorMessage*/
        this.requestService.getRequestById(this.requestId, this.authService.getUserEmail()).subscribe(
            res => {
                this.currentRequest = res;
            },
            error => {
                console.log(error);
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
                this.showSpinner = false;
            },
            () => {
                this.getIfUserCanEditRequest();
                if (this.currentRequest.stage === '1') {
                    this.createForm();
                }
            }
        );
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
        if (stage !== '13' && this.currentRequest.status !== 'rejected') {
            if (stage === '4') {
                return '5a';
            } else if (stage === '5a') {
                if (this.currentRequest.stage1.amountInEuros > this.amountLimit) {
                    return '5b';
                } else {
                    return '6';
                }
            } else if (stage === '5b') {
                return '6';
            } else {
                return (+stage + 1).toString();
            }
        }
        /*console.log('next stage is', newStage);*/
        return this.currentRequest.stage;
    }

    getPreviousStage(stage: string) {
        if (stage === '5a') {
            return '4';
        } else if (stage === '5b') {
            return '5a';
        } else if (stage === '6') {
            if (this.currentRequest.stage1.amountInEuros > this.amountLimit) {
                return '5b';
            } else {
                return '5a';
            }
        } else {
            return (+stage - 1).toString();
        }
    }

    getSubmittedStage(newStage: any) {
        console.log(`got ${JSON.stringify(newStage, null, 1)}`);
        const currentStageName = 'stage' + this.currentRequest.stage;
        console.log(`submitting as ${currentStageName}`);
        this.currentRequest[currentStageName] = newStage;
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
        this.wentBackOneStage = false;
        console.log('submitted status:', this.currentRequest.status);
        this.submitRequest();
    }

    submitRequest() {
        window.scrollTo(0, 0);
        this.showSpinner = true;
        this.errorMessage = '';
        this.successMessage = '';

        /*update this.currentRequest*/
        this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe(
            res => console.log(`update Request responded: ${res.id}, status=${res.status}, stage=${res.stage}`),
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
            },
            () => {
                this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν.';
                this.showSpinner = false;
            }
        );
    }

    willShowStage(stage: string) {
        if (!this.isSimpleUser) {
            if ((stage === this.currentRequest.stage)) {
                if (this.authService.getUserRole() === 'ROLE_ADMIN') {
                    return true;
                } else {
                    return this.canEdit;
                }
            } else {
                /*console.log('BOOM!');*/
                return (!isNullOrUndefined(this.currentRequest[`stage${stage}`]) &&
                    !isNullOrUndefined(this.currentRequest[`stage${stage}`].date) );
            }
        } else {
            return false;
        }
    }

    checkIfHasReturnedToPrevious(stage: string) {

        /* if stage is the pending stage */
        if ((this.currentRequest.stage === stage)) {

            /* if the request has been finalized return 0 */
            if (( (this.currentRequest.status === 'rejected') ||
                    (this.currentRequest.status === 'accepted') )) {
                return 0;

                /* else return 1 */
            } else {
                return 1;
            }
        } else {

            /* iterate the stages */
            for (const st of this.stages) {

                /* if stage is before the pending stage return 0 */
                if (st === stage) {
                    return 0;
                }

                /* if stage is after the pending stage return 2 */
                if (st === this.currentRequest.stage) {
                    return 2;
                }
            }
        }
    }

    linkToFile() {
        if (this.currentRequest.stage1.attachment && this.currentRequest.stage1.attachment.url) {
            window.open(this.currentRequest.stage1.attachment.url, '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
        }
    }


    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    showAmount() {
        this.requestedAmount = this.updateStage1Form.get('amount').value.trim();
        this.updateStage1Form.get('amount').updateValueAndValidity();
    }

    updateStage1() {
        console.log(this.updateStage1Form);
        this.errorMessage = '';
        if (this.updateStage1Form.valid ) {
            if ( (+this.updateStage1Form.get('amount').value > 2500) && isUndefined(this.uploadedFile) ) {
                this.errorMessage = 'Για αιτήματα άνω των 2.500 € η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else if ( ( this.currentRequest.type !== 'trip' ) &&
                !this.updateStage1Form.get('supplier').value &&
                !this.updateStage1Form.get('supplierSelectionMethod').value  ) {

                this.errorMessage = 'Χρειάζεται να προσθέσετε πληροφορίες για τον προμηθευτή.';
            } else if ( (( this.updateStage1Form.get('supplierSelectionMethod').value !== 'Απ\' ευθείας ανάθεση' ) &&
                    ( this.currentRequest.type !== 'trip' )) && isUndefined(this.uploadedFile)  ) {

                this.errorMessage = 'Για αναθέσεις μέσω διαγωνισμού ή έρευνας αγοράς η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else if ( (+this.updateStage1Form.get('amount').value > this.amountLimit) &&
                        ( this.updateStage1Form.get('supplierSelectionMethod').value !== 'Διαγωνισμός' ) ) {

                this.errorMessage = 'Για ποσά άνω των 20.000 € οι αναθέσεις πρέπει να γίνονται μέσω διαγωνισμού.';
            } else {
                this.currentRequest.stage1.requestDate = Date.now().toString();
                this.currentRequest.stage1.subject = this.updateStage1Form.get('requestText').value;
                this.currentRequest.stage1.supplier = this.updateStage1Form.get('supplier').value;
                this.currentRequest.stage1.supplierSelectionMethod = this.updateStage1Form.get('supplierSelectionMethod').value;
                this.currentRequest.stage1.amountInEuros = +this.updateStage1Form.get('amount').value;
                if (this.uploadedFile) {
                    this.currentRequest.stage1.attachment = new Attachment();
                    this.currentRequest.stage1.attachment.filename = this.uploadedFile.name;
                    this.currentRequest.stage1.attachment.mimetype = this.uploadedFile.type;
                    this.currentRequest.stage1.attachment.size = this.uploadedFile.size;
                    this.currentRequest.stage1.attachment.url = '';
                }
                this.currentRequest.stage = '2';
                this.currentRequest.status = 'pending';

                if ( (this.currentRequest.stage1.amountInEuros > this.amountLimit) && isUndefined(this.currentRequest.stage5b) ) {
                    this.currentRequest.stage5b = new Stage5b();
                }

                this.submitRequest();
            }

        } else {
            this.errorMessage = 'Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά';
        }
    }

}
