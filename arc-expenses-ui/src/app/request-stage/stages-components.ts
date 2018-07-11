import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Attachment, User, POI, Project } from '../domain/operation';
import { commentDesc, StageFieldDescription, stagesDescriptionMap } from '../domain/stageDescriptions';
import { DatePipe } from '@angular/common';
import { AuthenticationService } from '../services/authentication.service';
import { isNullOrUndefined, isUndefined } from 'util';

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

    /* input variable that controls if the current stage template should be displayed */
    @Input() showStage: number; /* values: 0 -> don't show
                                           1 -> show form
                                           2 -> was approved
                                           3 -> was rejected
                                           4 -> was returned to previous*/

    @Input() oldSupplierAndAmount: string[];
    @Input() requester: User;
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

    @Input() currentStage: any;
    @Input() currentProject: Project;
    @Input() currentRequestId: string;
    currentPOI: POI;

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder,
                private authService: AuthenticationService) {}

    ngOnInit() {
        this.stageTitle = stagesDescriptionMap[this.stageId].title;
        this.stageFields = stagesDescriptionMap[this.stageId].stageFields;

        if ( !isNullOrUndefined(this.data)) {
            this.parseData();
        }
        this.initializeView();
    }

    parseData() {
        if ( !isNullOrUndefined(this.data) ) {
            this.showStage = this.data['showStage'];
            this.currentStage = this.data['currentStage'];
            this.currentProject = this.data['currentProject'];
            this.oldSupplierAndAmount = this.data['oldSupplierAndAmount'];
            this.requester = this.data['requester'];
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

    findCurrentPOI(poiList: POI[]) {
        if (this.authService.getUserRole() === 'ROLE_ADMIN') {
            return poiList[0];
        } else {
            let curEmail: string;
            if ( this.showStage === 1 ) {
                curEmail = this.authService.getUserProp('email');
            } else {
                curEmail = this.currentStage['user']['email'];
            }
            for ( const poi of poiList ) {
                if ( (poi.email === curEmail) || poi.delegates.some(x => x.email === curEmail) ) {
                    return poi;
                }
            }
        }
    }

    linkToFile() {
        if (this.currentStage['attachment'] && this.currentStage['attachment']['url'].length > 0 ) {
            /*window.open(this.currentStage['attachment']['url'], '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');*/
            window.open(`${window.location.origin}/arc-expenses-service/request/store/download?requestId=${this.currentRequestId}&stage=${this.stageId}`,
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

    getDelegateName() {
        if ( this.getIsDelegateHidden() ) {
            return this.currentPOI.firstname + ' ' + this.currentPOI.lastname;
        } else {
            return this.currentStage['user']['firstname'] + ' ' + this.currentStage['user']['lastname'];
        }
    }

    getUserName() {
        return this.requester.firstname + ' ' + this.requester.lastname;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον επιστημονικό υπεύθυνο';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον επιστημονικό υπεύθυνο';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον επιστημονικό υπεύθυνο';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.scientificCoordinator;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον χειριστή του προγράμματος';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον χειριστή του προγράμματος';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον χειριστή του προγράμματος';
        }

        super.ngOnInit();

        this.currentPOI = this.findCurrentPOI(this.currentProject.operator);
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.organization.POI;
    }
}

/* NOT USED ANYMORE  -- MAYBE WE'LL RESTORE IT AT SOME POINT */
@Component ({
    selector: 'stage5-component',
    templateUrl: './stages-components.html'
})
export class Stage5Component extends StageComponent implements OnInit {
    @Input() willShowButtonTo5a: boolean;
    @Input() willShowButtonTo5b: boolean;

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '5';
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον Διευθυντή του Ινστιτούτου';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον Διευθυντή του Ινστιτούτου';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον Διευθυντή του Ινστιτούτου';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.director;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον Διατάκτη';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον Διατάκτη';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον Διατάκτη';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.organization.director;
    }
}

@Component ({
    selector: 'stageUploadInvoice-component',
    templateUrl: './stages-templates/stageUploadInvoice.component.html'
})
export class StageUploadInvoiceComponent extends StageComponent implements OnInit {
    amountNaN: boolean;

    ngOnInit () {
        console.log('oldSupplierAndAmount is', this.oldSupplierAndAmount);
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = 'UploadInvoice';
        if (this.showStage > 1) {
            this.submittedStageResult = 'Η υποβολή του τιμολογίου έγινε από τον αιτούντα';
        }

        super.ngOnInit();
        this.currentPOI = null;
    }

    showAmount(event: any) {
        this.oldSupplierAndAmount[1] = event.target.value;

        if (this.oldSupplierAndAmount[1].includes(',')) {

            const temp = this.oldSupplierAndAmount[1].replace(',', '.');
            this.oldSupplierAndAmount[1] = temp;
        }

        this.amountNaN = isNaN(+this.oldSupplierAndAmount[1]);
    }

    emitNewValues() {
        this.stageFormError = '';
        this.amountNaN = isNaN(+this.oldSupplierAndAmount[1]);
        console.log(`emitting values: amountNaN is ${this.amountNaN}`);
        if ( !isUndefined(this.oldSupplierAndAmount) ) {

            if (!isNullOrUndefined(this.oldSupplierAndAmount[0]) &&
                !isNullOrUndefined(this.oldSupplierAndAmount[1]) &&
                !this.amountNaN &&
                (this.oldSupplierAndAmount[0].length > 0) &&
                (this.oldSupplierAndAmount[1].length > 0)) {

                const newValArray = [];
                newValArray.push(this.oldSupplierAndAmount[0]);
                newValArray.push(this.oldSupplierAndAmount[1]);
                this.newValues.emit(newValArray);
                this.submitForm();
            } else {
                this.stageFormError = 'Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά.';
            }

        }
    }

    /*emitNewValuesAndGoBack() {
        if ( !isUndefined(this.oldSupplierAndAmount) ) {
            if ( !isNullOrUndefined(this.oldSupplierAndAmount[0]) ||
                 !isNullOrUndefined(this.oldSupplierAndAmount[1]) ) {

                const newValArray = [];
                newValArray.push(this.oldSupplierAndAmount[0]);
                newValArray.push(this.oldSupplierAndAmount[1]);
                this.newValues.emit(newValArray);
            }
        }
        this.goBackOneStage();
    }*/

    updateSupplier(event: any) {
        this.oldSupplierAndAmount[0] = event.target.value;
    }

    updateAmount(event: any) {
        this.amountNaN = isNaN(+event.target.value);
        if (!this.amountNaN) {
            this.oldSupplierAndAmount[1] = event.target.value;
        }
    }

}

@Component ({
    selector: 'stage5b-component',
    templateUrl: './stages-templates/stage5b.component.html'
})
export class Stage5bComponent extends StageComponent implements OnInit {
    amountNaN: boolean;

    ngOnInit () {
        console.log('oldSupplierAndAmount is', this.oldSupplierAndAmount);
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '5b';
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από το Διοικητικό Συμβούλιο';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από το Διοικητικό Συμβούλιο';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από το Διοικητικό Συμβούλιο';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.organization.dioikitikoSumvoulio;
    }

    showAmount(event: any) {
        this.oldSupplierAndAmount[1] = event.target.value;

        if (this.oldSupplierAndAmount[1].includes(',')) {

            const temp = this.oldSupplierAndAmount[1].replace(',', '.');
            this.oldSupplierAndAmount[1] = temp;
        }

        this.amountNaN = isNaN(+this.oldSupplierAndAmount[1]);
    }

    emitNewValues(approved: boolean) {
        this.stageFormError = '';
        if ( !isUndefined(this.oldSupplierAndAmount) ) {

            if (!approved) {
                if (!isNullOrUndefined(this.oldSupplierAndAmount[0]) ||
                    !isNullOrUndefined(this.oldSupplierAndAmount[1]) ) {

                    const newValArray = [];
                    newValArray.push(this.oldSupplierAndAmount[0]);
                    newValArray.push(this.oldSupplierAndAmount[1]);
                    this.newValues.emit(newValArray);
                }
                this.approveRequest(approved);

            } else if ( !isNullOrUndefined(this.oldSupplierAndAmount[0]) &&
                        !isNullOrUndefined(this.oldSupplierAndAmount[1]) &&
                        !this.amountNaN &&
                        (this.oldSupplierAndAmount[0].length > 0) &&
                        (this.oldSupplierAndAmount[1].length > 0) ) {

                const newValArray = [];
                newValArray.push(this.oldSupplierAndAmount[0]);
                newValArray.push(this.oldSupplierAndAmount[1]);
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
        if ( !isUndefined(this.oldSupplierAndAmount) ) {
            if (!isNullOrUndefined(this.oldSupplierAndAmount[0]) ||
                !isNullOrUndefined(this.oldSupplierAndAmount[1]) ) {

                const newValArray = [];
                newValArray.push(this.oldSupplierAndAmount[0]);
                newValArray.push(this.oldSupplierAndAmount[1]);
                this.newValues.emit(newValArray);
            }
        }
        this.goBackOneStage();
    }

    updateSupplier(event: any) {
        this.oldSupplierAndAmount[0] = event.target.value;
    }

    updateAmount(event: any) {
        this.amountNaN = isNaN(+event.target.value);
        if (!this.amountNaN) {
            this.oldSupplierAndAmount[1] = event.target.value;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Αναρτήθηκε στην ΔΙΑΥΓΕΙΑ';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.diaugeia;
    }
}


@Component ({
    selector: 'stage7-component',
    templateUrl: './stages-templates/stage7.component.html'
})
export class Stage7Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };
        this.stageId = '7';
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από το Γραφείο Προμηθειών';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από το Γραφείο Προμηθειών';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από το Γραφείο Προμηθειών';
        }

        super.ngOnInit();
        /*this.currentPOI = this.findCurrentPOI(this.currentProject.operator);*/
        this.currentPOI = this.currentProject.institute.suppliesOffice;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από την Ομάδα Ελέγχου';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από την Ομάδα Ελέγχου';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από την Ομάδα Ελέγχου';
        }

        super.ngOnInit();
        this.currentPOI = this.findCurrentPOI(this.currentProject.institute.organization.inspectionTeam);
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον Προϊστάμενο Οικονομικών Υπηρεσιών';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.organization.POI;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε από τον Διατάκτη';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε από τον Διατάκτη';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο από τον Διατάκτη';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.organization.director;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Αναρτήθηκε στην ΔΙΑΥΓΕΙΑ';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ';
        }


        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.diaugeia;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.accountingRegistration;
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
        if (this.showStage === 2) {
            // was approved phrase
            this.submittedStageResult = 'Εγκρίθηκε';
        } else if (this.showStage === 3) {
            // was not approved phrase
            this.submittedStageResult = 'Απορρίφθηκε';
        } else if (this.showStage === 4) {
            // was returned phrase
            this.submittedStageResult = 'Επεστράφη στο προηγούμενο στάδιο';
        }

        super.ngOnInit();
        this.currentPOI = this.currentProject.institute.accountingPayment;
    }
}
