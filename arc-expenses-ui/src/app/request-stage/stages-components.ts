import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Attachment, User, POI, Project, Request } from '../domain/operation';
import {
    commentDesc, Stage10Desc, Stage2Desc, Stage5aDesc, Stage5bDesc, Stage3Desc, Stage4Desc, Stage5Desc, Stage6Desc,
    Stage7Desc, Stage8Desc, Stage9Desc, StageDescription, StageFieldDescription, Stage12Desc, stagesDescriptionMap, Stage11Desc,
    Stage13Desc
} from '../domain/stageDescriptions';
import { DatePipe } from '@angular/common';
import { AuthenticationService } from '../services/authentication.service';
import { Router } from '@angular/router';
import { isNullOrUndefined, isUndefined } from 'util';
import {ManageRequestsService} from '../services/manage-requests.service';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {
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
    @Input() showStage: boolean;

    /* input variable that controls if in this stage the user chose to return the request back to the previous stage */
    @Input() hasReturnedToPrevious: number;

    /* if true, the results of the stage will be shown
     * otherwise a form will be shown so that the user can update the request */
    wasSubmitted: boolean;

    /* if the stage was submitted in the past, this variable will control the displayed
     * text according to the submitted stage's status [approved/rejected] */
    wasApproved: string;

    /*  phrase mentioning the delegate's position.
        It is used to describe the results of the stage*/
    delegatePositionInParagraph: string;

    stageForm: FormGroup;
    stageTitle: string;

    stageFormDefinition; /*will contain the form schema*/
    uploadedFile: File;
    uploadedFilename: string = ''; /*a filename to send to the attachment wrapper*/

    @Input() currentStage: any;
    @Input() currentProject: Project;
    currentPOI: POI;
    stageDescription: StageDescription;  /*contains the name of the delegate field and the list of the extra fields descriptions*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder,
                private authService: AuthenticationService,
                private requestService: ManageRequestsService) {}

    ngOnInit() {
        /*console.log(`showStage ${this.stageDescription.id} is ${this.showStage}`);*/
        this.checkIfSubmitted();
        this.checkIfApproved();
        this.stageTitle = stagesDescriptionMap[this.stageDescription.id].title;
    }

    checkIfSubmitted() {
        /*console.log(`hasReturned is ${this.hasReturnedToPrevious}`);*//*console.log(`hasReturned is ${this.hasReturnedToPrevious}`);*/
        this.wasSubmitted = ( (!isNullOrUndefined(this.currentStage) &&
            !isNullOrUndefined(this.currentStage.date)) &&
            (this.hasReturnedToPrevious !== 1) );
        if ( !this.wasSubmitted && (this.hasReturnedToPrevious !== 2) && ( this.authService.getUserRole() !== 'ROLE_USER' )) {
            if (!this.showStage) {
                this.wasSubmitted = true;
            } else {
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
        /*console.log(`in stage ${this.stageDescription.id}, wasSubmitted is ${this.wasSubmitted}`);*/
    }

    checkIfApproved() {
        if (this.currentStage) {
            if (this.hasReturnedToPrevious === 2) {
                this.wasApproved = 'Επεστράφη στο προηγούμενο στάδιο';
            } else {
                if ( this.currentStage['approved'] ||
                    ( this.stageDescription &&
                        ( this.stageDescription.id === '11' ) ) ) {

                  this.wasApproved = 'Εγκρίθηκε';

                } else if (this.stageDescription &&
                           ( (this.stageDescription.id === '6') || ( this.stageDescription.id === '11' ))  ) {

                  this.wasApproved = 'Αναρτήθηκε';

                } else {

                    this.wasApproved = 'Απορρίφθηκε';
                }
            }
        }
    }

    findCurrentPOI(poiList: POI[]) {
        if (this.authService.getUserRole() === 'ROLE_ADMIN') {
            return poiList[0];
        } else {
            return poiList.filter(x => x.email === this.authService.getUserEmail())[0];
        }
    }

    linkToFile() {
        if (this.currentStage['attachment'] && this.currentStage['attachment']['url'].length > 0 ) {
            window.open(this.currentStage['attachment']['url'], '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
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
            this.emitGoBack.emit(true);
            this.choseToGoBack = true;
            this.submitForm();
        }
    }

    submitForm() {
        this.stageFormError = '';
        if (this.stageForm && this.stageForm.valid && this.delegateCanEdit() ) {
            if ( (isNullOrUndefined(this.uploadedFile) && isNullOrUndefined(this.currentStage['attachment'])) &&
                !this.choseToGoBack &&
                ( (this.stageDescription.id === '6') || (this.stageDescription.id === '11') ||
                  ( (this.stageDescription.id === '7') && this.currentStage['approved']) ) ) {

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
                /*console.log(this.currentStage);*/
                /*this.checkIfSubmitted();
                this.checkIfApproved();*/
                this.emitStage.emit(this.currentStage);

            }
        } else {
            this.stageFormError = 'Πρέπει να έχουν γίνει όλοι οι έλεγχοι για να προχωρήσει το αίτημα.';
        }
    }

    delegateCanEdit() {
        return ( (this.authService.getUserRole() === 'ROLE_ADMIN') ||
            (this.currentPOI.email === this.authService.getUserEmail()) ||
            this.currentPOI.delegates.some(x => x.email === this.authService.getUserEmail()) );
    }

    createUser(): User {
        const tempUser: User = new User();
        if (this.authService.getUserRole() === 'ROLE_ADMIN') {
            tempUser.id = this.authService.getUserId();
            tempUser.email = this.currentPOI.email;
            tempUser.firstname = this.currentPOI.firstname;
            tempUser.lastname = this.currentPOI.lastname;
        } else {
            tempUser.id = this.authService.getUserId();
            tempUser.email = this.authService.getUserEmail();
            tempUser.firstname = this.authService.getUserFirstName();
            tempUser.lastname = this.authService.getUserLastName();
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
    templateUrl: './stages-templates/stage2.component.html'
})
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
            checkNecessity: ['', Validators.requiredTrue],
            checkFeasibility: ['', Validators.requiredTrue]
        };

        this.stageDescription = Stage2Desc;
        this.currentPOI = this.currentProject.scientificCoordinator;
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
            comment: [''],
            analiftheiYpoxrewsi: ['', Validators.requiredTrue],
            fundsAvailable: ['', Validators.requiredTrue],
            loan: [''],
            loanSource: ['']
        };

        this.stageDescription = Stage3Desc;
        this.currentPOI = this.findCurrentPOI(this.currentProject.operator);

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
    templateUrl: './stages-templates/stage4.component.html'
})
export class Stage4Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
            analiftheiYpoxrewsi: ['', Validators.requiredTrue],
            fundsAvailable: ['', Validators.requiredTrue],
        };

        this.stageDescription = Stage4Desc;
        this.currentPOI = this.currentProject.institute.organization.POI;
        super.ngOnInit();
    }
}

/* NOT USED ANYMORE  -- MAYBE WE'LL RESTORE IT AT SOME POINT */
@Component ({
    selector: 'stage5-component',
    templateUrl: './stages-templates/stage5.component.html'
})
export class Stage5Component extends StageComponent implements OnInit {
    @Input() willShowButtonTo5a: boolean;
    @Input() willShowButtonTo5b: boolean;

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage5Desc;
        this.currentPOI = this.currentProject.institute.director;
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage5a-component',
    templateUrl: './stages-templates/stage5a.component.html'
})
export class Stage5aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage5aDesc;
        this.currentPOI = this.currentProject.institute.organization.director;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage5b-component',
    templateUrl: './stages-templates/stage5b.component.html'
})
export class Stage5bComponent extends StageComponent implements OnInit {

    @Input() oldSupplierAndAmount: string[];
    @Output() newValues: EventEmitter<string[]> = new EventEmitter<string[]>();

    ngOnInit () {
        console.log('oldSupplierAndAmount is', this.oldSupplierAndAmount);
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage5bDesc;
        this.currentPOI = this.currentProject.institute.organization.dioikitikoSumvoulio;
        super.ngOnInit();
    }

    emitNewValues(approved: boolean) {
        this.stageFormError = '';
        if ( !isUndefined(this.oldSupplierAndAmount) ) {

            if ( !isNullOrUndefined(this.oldSupplierAndAmount[0]) &&
                 !isNullOrUndefined(this.oldSupplierAndAmount[1]) &&
                 (this.oldSupplierAndAmount[0].length > 0) &&
                 (this.oldSupplierAndAmount[1].length > 0)) {

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
            if ( !isNullOrUndefined(this.oldSupplierAndAmount[0]) ||
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
        this.oldSupplierAndAmount[1] = event.target.value;
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

        this.stageDescription = Stage6Desc;
        this.currentPOI = this.currentProject.institute.diaugeia;
        super.ngOnInit();
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

        this.stageDescription = Stage7Desc;
        this.currentPOI = this.findCurrentPOI(this.currentProject.operator);
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage8-component',
    templateUrl: './stages-templates/stage8.component.html'
})
export class Stage8Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            checkRegularity: ['', Validators.requiredTrue],
            checkLegality: ['', Validators.requiredTrue],
            comment: [''],
        };

        this.stageDescription = Stage8Desc;
        this.currentPOI = this.findCurrentPOI(this.currentProject.institute.organization.inspectionTeam);
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage9-component',
    templateUrl: './stages-templates/stage9.component.html'
})
export class Stage9Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            checkRegularity: ['', Validators.requiredTrue],
            checkLegality: ['', Validators.requiredTrue],
            comment: [''],
        };

        this.stageDescription = Stage9Desc;
        this.currentPOI = this.currentProject.institute.organization.POI;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage10-component',
    templateUrl: './stages-templates/stage10.component.html'
})
export class Stage10Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage10Desc;
        this.currentPOI = this.currentProject.institute.organization.director;
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

        this.stageDescription = Stage11Desc;
        this.currentPOI = this.currentProject.institute.diaugeia;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage12-component',
    templateUrl: './stages-templates/stage12.component.html'
})
export class Stage12Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage12Desc;
        this.currentPOI = this.currentProject.institute.accountingRegistration;
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage13-component',
    templateUrl: './stages-templates/stage13.component.html'
})
export class Stage13Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            comment: [''],
        };

        this.stageDescription = Stage13Desc;
        this.currentPOI = this.currentProject.institute.accountingPayment;
        super.ngOnInit();
    }
}
