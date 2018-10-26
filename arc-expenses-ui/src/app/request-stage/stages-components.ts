import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Attachment, User, PersonOfInterest } from '../domain/operation';
import { commentDesc, StageFieldDescription } from '../domain/stageDescriptions';
import { DatePipe } from '@angular/common';
import { AuthenticationService } from '../services/authentication.service';
import { isNullOrUndefined, isUndefined } from 'util';
import { RequestInfo, StageInfo } from '../domain/requestInfoClasses';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {
    @Input() data: any;

    stageFormError: string;

    /* output variable that sends the new Stage back to the parent component
     * in order to call the api and update the request */
    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();
    @Output() emitFile: EventEmitter<File> = new EventEmitter<File>();

    /* output variable that sends back to the parent an alert that the user
     * chose to go back to the previous stage */
    @Output() emitGoBack: EventEmitter<any> = new EventEmitter<any>();
    choseToGoBack: boolean;

    /* input variable that defines the status of the current stage */
    showStage: number; /* values:  0 -> don't show
                                   1 -> show form
                                   2 -> was approved
                                   3 -> was rejected
                                   4 -> was returned to previous*/

    @Output() newValues: EventEmitter<string[]> = new EventEmitter<string[]>();


    /*  phrase mentioning the result of a submitted stage
        it changes according to stage */
    submittedStageResult: string;

    stageForm: FormGroup;
    stageFormDefinition; /*will contain the form schema*/
    commentFieldDesc: StageFieldDescription = commentDesc;

    stageTitle: string;
    stageId: string;
    stageFields: StageFieldDescription[];

    uploadedFile: File;
    uploadedFilename = ''; /*a filename to send to the attachment wrapper*/

    currentStage: any;
    currentRequestInfo: RequestInfo;
    currentStageInfo: StageInfo;

    currentPOI: PersonOfInterest;

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder,
                private authService: AuthenticationService) {}

    ngOnInit() {

        this.parseData();

        if (!isNullOrUndefined(this.stageId) &&
            !isNullOrUndefined(this.currentRequestInfo)) {

            this.currentStageInfo = this.currentRequestInfo[this.stageId];
            this.showStage = this.currentStageInfo.showStage;
            this.stageTitle = this.currentStageInfo.title;
            this.stageFields = this.currentStageInfo.stageFields;
            if (this.showStage > 1) {
                this.submittedStageResult = this.currentStageInfo.submittedStageResultMap[this.showStage.toString()];
            }

            this.initializeView();
            this.currentPOI = this.findCurrentPOI();
        }
    }

    parseData() {
        if ( !isNullOrUndefined(this.data) ) {
            this.currentStage = this.data['currentStage'];
            this.currentRequestInfo = this.data['currentRequestInfo'];
        }
    }

    initializeView() {
        if (this.showStage === 1) {

            /* set filename if exists */
            if ( !isNullOrUndefined(this.currentStage['attachment']) ) {
                this.uploadedFilename = this.currentStage['attachment']['filename'];
            }

            /* create form */
            this.stageForm = this.fb.group(this.stageFormDefinition);

            /* fill the form if the values exist */
            Object.keys(this.stageForm.controls).forEach(key => {
                if (!isNullOrUndefined(this.currentStage[key.toString()])) {
                    this.stageForm.get(key).setValue(this.currentStage[key.toString()]);
                }
            });
        }
    }

    findCurrentPOI() {
        if ((this.currentStageInfo.stagePOIs.length === 1) || (this.authService.getUserRole() === 'ROLE_ADMIN') ) {
            return this.currentStageInfo.stagePOIs[0];
        } else {
            let curEmail: string;
            if ( this.showStage === 1 ) {
                curEmail = this.authService.getUserProp('email');
            } else {
                curEmail = this.currentStage['user']['email'];
            }
            for ( const poi of this.currentStageInfo.stagePOIs ) {
                if ( (poi.email === curEmail) || poi.delegates.some(x => x.email === curEmail) ) {
                    return poi;
                }
            }
        }
    }

    linkToFile() {
        if (this.currentStage['attachment'] && this.currentStage['attachment']['url'].length > 0 ) {
            /* direct link to the storeService */
            /*window.open(this.currentStage['attachment']['url'], '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');*/
            const mode: string = (this.currentRequestInfo.phaseId.includes('a') ? 'approval' : 'payment');
            let url = `${window.location.origin}/arc-expenses-service/request/store/download?requestId=`;
            url = `${url}${this.currentRequestInfo.phaseId}&stage=${this.stageId}&mode=${mode}`;
            console.log(url);
            /* link to download method */
            window.open(url,
                      '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
        }
    }

    getAttachmentInput(newFile: File) {
        this.stageFormError = '';
        this.uploadedFile = newFile;
        console.log('this.uploadedFile is : ', this.uploadedFile);
    }

    approveRequest( approved: boolean ) {
        console.log('approved is:', approved);
        if (!approved) {
            Object.keys(this.stageForm.controls).forEach(key => {
                this.stageForm.get(key).clearValidators();
                this.stageForm.get(key).updateValueAndValidity();
            });
        }
        this.currentStage['approved'] = approved;

        this.submitForm();
    }

    goBackOneStage() {
        this.stageFormError = '';
        if ( !this.stageForm.get('comment').value ) {
            this.stageFormError = 'Είναι υποχρεωτικό να γράψετε ένα σχόλιο για την επιλογή σας.';
        } else {
            Object.keys(this.stageForm.controls).forEach(key => {
                this.stageForm.get(key).clearValidators();
                this.stageForm.get(key).updateValueAndValidity();
            });
            this.choseToGoBack = true;
            this.emitGoBack.emit( this.choseToGoBack );
            this.submitForm();
        }
    }

    submitForm() {
        this.stageFormError = '';
        if (this.stageForm && this.stageForm.valid ) {
            if ( (isNullOrUndefined(this.uploadedFile) && isNullOrUndefined(this.currentStage['attachment'])) &&
                 !this.choseToGoBack &&
                 ( (this.stageId === '6') || (this.stageId === '11') ||
                   ( (this.stageId === '7') && this.currentStage['approved']) ) ) {

                this.stageFormError = 'Η επισύναψη εγγράφων είναι υποχρεωτική.';

            } else {

                this.currentStage['user'] = this.createUser();

                this.currentStage['date'] = Date.now().toString();
                Object.keys(this.stageForm.controls).forEach(key => {
                    this.currentStage[key.toString()] = this.stageForm.get(key).value;
                });

                if (this.uploadedFile) {
                    this.currentStage['attachment'] = this.createAttachment();
                    this.emitFile.emit(this.uploadedFile);

                }
                this.emitStage.emit(this.currentStage);

            }

        } else {

            this.stageFormError = 'Πρέπει να έχουν γίνει όλοι οι έλεγχοι για να προχωρήσει το αίτημα.';
        }
    }

    createUser(): User {
        const tempUser: User = new User();
        if (this.authService.getUserRole() === 'ROLE_ADMIN') {
            tempUser.id = this.authService.getUserProp('id');
            tempUser.email = this.currentPOI.email;
            tempUser.firstname = this.currentPOI.firstname;
            tempUser.lastname = this.currentPOI.lastname;
        } else {
            tempUser.id = this.authService.getUserProp('id');
            tempUser.email = this.authService.getUserProp('email');
            tempUser.firstname = this.authService.getUserProp('firstname');
            tempUser.lastname = this.authService.getUserProp('lastname');
        }
        return tempUser;
    }

    getIsDelegateHidden() {
        if (this.currentPOI.email === this.currentStage['user']['email']) {
            return false;
        } else {
            return this.currentPOI.delegates.filter(x => x.email === this.currentStage['user']['email'])[0].hidden;
        }
    }

    /* display full name of the submitted stage's editor */
    getDelegateName() {
        if (this.stageId === '7') {
            return this.currentStage['user']['firstname'] + ' ' + this.currentStage['user']['lastname'];
        } else {
            if ( this.getIsDelegateHidden() ) {
                return this.currentPOI.firstname + ' ' + this.currentPOI.lastname;
            } else {
                return this.currentStage['user']['firstname'] + ' ' + this.currentStage['user']['lastname'];
            }
        }
    }

    getUserName() {
        if (!isNullOrUndefined(this.currentRequestInfo.requester)) {
            return this.currentRequestInfo.requester.firstname + ' ' + this.currentRequestInfo.requester.lastname;
        } else {
            return '';
        }
    }

    createAttachment(): Attachment {
        const tempAttachment: Attachment = new Attachment();
        if (this.uploadedFile) {
            tempAttachment.filename = this.uploadedFile.name;
            tempAttachment.mimetype = this.uploadedFile.type;
            tempAttachment.size = this.uploadedFile.size;
            tempAttachment.url = '';
        }

        return tempAttachment;
    }

    getCurrentDateString() {
        return this.datePipe.transform(Date.now(), 'dd/MM/yyyy');
    }

}

@Component ({
    selector: 'stage2-component',
    templateUrl: './stages-components.html'
})
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            checkNecessity: ['', Validators.requiredTrue],
            checkFeasibility: ['', Validators.requiredTrue],
            comment: ['']
        };
        this.stageId = '2';

        super.ngOnInit();
    }

}


@Component ({
    selector: 'stage3-component',
    templateUrl: './stages-templates/stage3.component.html'
})
export class Stage3Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            analiftheiYpoxrewsi: ['', Validators.requiredTrue],
            fundsAvailable: ['', Validators.requiredTrue],
            loan: [''],
            loanSource: [''],
            comment: ['']
        };
        this.stageId = '3';

        super.ngOnInit();

        if (this.stageForm) {
            this.stageForm.get('loanSource').disable();
        }
    }

    onLoanToggle (completedLoan: boolean) {
        if (completedLoan && !isUndefined(this.stageForm) ) {
            this.stageForm.get('loanSource').enable();
            this.stageForm.get('loanSource').setValidators([Validators.required]);
            this.stageForm.get('loanSource').updateValueAndValidity();
        } else {
            this.stageForm.get('loanSource').disable();
        }
    }
}

@Component ({
    selector: 'stage4-component',
    templateUrl: './stages-components.html'
})
export class Stage4Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            analiftheiYpoxrewsi: ['', Validators.requiredTrue],
            fundsAvailable: ['', Validators.requiredTrue],
            comment: ['']
        };
        this.stageId = '4';

        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage5a-component',
    templateUrl: './stages-components.html'
})
export class Stage5aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '5a';

        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage5b-component',
    templateUrl: './stages-templates/stage5b.component.html'
})
export class Stage5bComponent extends StageComponent implements OnInit {
    amountNaN: boolean;
    showExtraFields: boolean;

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '5b';

        super.ngOnInit();

        if (!isUndefined(this.currentRequestInfo.supplier) && !isUndefined(this.currentRequestInfo.requestedAmount)) {
            this.showExtraFields = true;
            console.log('oldSupplierAndAmount are', this.currentRequestInfo.supplier, 'and', this.currentRequestInfo.requestedAmount);
        }

    }

    showAmount(event: any) {
        if (this.showExtraFields) {
            this.currentRequestInfo.requestedAmount = event.target.value;

            if (this.currentRequestInfo.requestedAmount.includes(',')) {

                const temp = this.currentRequestInfo.requestedAmount.replace(',', '.');
                this.currentRequestInfo.requestedAmount = temp;
            }

            this.amountNaN = isNaN(+this.currentRequestInfo.requestedAmount);
        }
    }

    emitNewValues(approved: boolean) {
        this.stageFormError = '';
        if ( this.showExtraFields ) {

            if (!approved) {
                if (!isNullOrUndefined(this.currentRequestInfo.supplier) ||
                    !isNullOrUndefined(this.currentRequestInfo.requestedAmount) ) {

                    const newValArray = [];
                    newValArray.push(this.currentRequestInfo.supplier);
                    newValArray.push(this.currentRequestInfo.requestedAmount);
                    this.newValues.emit(newValArray);
                }
                this.approveRequest(approved);

            } else if ( !isNullOrUndefined(this.currentRequestInfo.supplier) &&
                        !isNullOrUndefined(this.currentRequestInfo.requestedAmount) &&
                        !this.amountNaN &&
                        (this.currentRequestInfo.supplier.length > 0) &&
                        (this.currentRequestInfo.requestedAmount.length > 0) ) {

                const newValArray = [];
                newValArray.push(this.currentRequestInfo.supplier);
                newValArray.push(this.currentRequestInfo.requestedAmount);
                this.newValues.emit(newValArray);
                this.approveRequest(approved);

            } else {
                this.stageFormError = 'Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.';
            }

        } else {
            this.approveRequest(approved);
        }
    }

    emitNewValuesAndGoBack() {
        if ( this.showExtraFields ) {
            if (!isNullOrUndefined(this.currentRequestInfo.supplier) ||
                !isNullOrUndefined(this.currentRequestInfo.requestedAmount) ) {

                const newValArray = [];
                newValArray.push(this.currentRequestInfo.supplier);
                newValArray.push(this.currentRequestInfo.requestedAmount);
                this.newValues.emit(newValArray);
            }
        }
        this.goBackOneStage();
    }

    updateSupplier(event: any) {
        this.currentRequestInfo.supplier = event.target.value;
    }

    updateAmount(event: any) {
        this.amountNaN = isNaN(+event.target.value);
        if (!this.amountNaN) {
            this.currentRequestInfo.requestedAmount = event.target.value;
        }
    }
}

@Component ({
    selector: 'stage6-component',
    templateUrl: './stages-templates/stage6.component.html'
})
export class Stage6Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '6';

        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage7-component',
    templateUrl: './stages-templates/stage7.component.html'
})
export class Stage7Component extends StageComponent implements OnInit {
    amountNaN: boolean;
    showExtraFields: boolean;

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '7';

        super.ngOnInit();

        if (!isUndefined(this.currentRequestInfo.finalAmount)) {
            this.showExtraFields = true;
            console.log('oldFinalAmount is', this.currentRequestInfo.finalAmount);
        }
    }


    showAmount(event: any) {
        if (this.showExtraFields) {
            this.currentRequestInfo.finalAmount = event.target.value;

            if (this.currentRequestInfo.finalAmount.includes(',')) {

                const temp = this.currentRequestInfo.finalAmount.replace(',', '.');
                this.currentRequestInfo.finalAmount = temp;
            }

            this.amountNaN = isNaN(+this.currentRequestInfo.finalAmount);
        }
    }

    emitNewValuesAndForward() {
        this.stageFormError = '';
        if ( this.showExtraFields ) {
            if ( !isNullOrUndefined(this.currentRequestInfo.finalAmount) &&
                !this.amountNaN && (this.currentRequestInfo.finalAmount.length > 0) ) {

                const newValArray = [];
                newValArray.push(this.currentRequestInfo.finalAmount);
                this.newValues.emit(newValArray);
                this.approveRequest(true);

            } else {
                this.stageFormError = 'Παρακαλώ συμπληρώστε ένα τελικό ποσό.';
            }

        } else {
            this.approveRequest(true);
        }
    }

    updateAmount(event: any) {
        this.amountNaN = isNaN(+event.target.value);
        if (!this.amountNaN) {
            this.currentRequestInfo.finalAmount = event.target.value;
        }
    }

}

@Component ({
    selector: 'stage8-component',
    templateUrl: './stages-components.html'
})
export class Stage8Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            checkRegularity: ['', Validators.requiredTrue],
            checkLegality: ['', Validators.requiredTrue],
            comment: [''],
        };
        this.stageId = '8';

        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage9-component',
    templateUrl: './stages-components.html'
})
export class Stage9Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            checkRegularity: ['', Validators.requiredTrue],
            checkLegality: ['', Validators.requiredTrue],
            comment: [''],
        };
        this.stageId = '9';

        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage10-component',
    templateUrl: './stages-components.html'
})
export class Stage10Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '10';

        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage11-component',
    templateUrl: './stages-templates/stage11.component.html'
})
export class Stage11Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '11';

        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage12-component',
    templateUrl: './stages-components.html'
})
export class Stage12Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '12';

        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage13-component',
    templateUrl: './stages-components.html'
})
export class Stage13Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '13';

        super.ngOnInit();
    }
}
