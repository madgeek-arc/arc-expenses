import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';
import {Attachment, User, POI, Project} from '../domain/operation';
import {
    commentDesc, Stage10Desc, Stage2Desc, Stage5aDesc, Stage5bDesc, Stage3Desc, Stage4Desc, Stage5Desc, Stage6Desc,
    Stage7Desc, Stage8Desc, Stage9Desc, StageDescription, StageFieldDescription, Stage12Desc, stagesMap, Stage11Desc,
    Stage13Desc
} from '../domain/stageDescriptions';
import {DatePipe} from '@angular/common';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';
import { isNullOrUndefined, isUndefined } from 'util';

declare const UIkit: any;

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {
    stageFormError: string;

    /* output variable that sends the new Stage back to the parent component
     * in order to call the api and update the request */
    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();

    /* output variable that sends back to the parent an alert that the user
     * chose to go back to the previous stage */
    @Output() emitGoBack: EventEmitter<any> = new EventEmitter<any>();

    /* OUT OF USE */
    /* sends the next stage id back to the parent component*/
    /*@Output() emitNextStage: EventEmitter<string> = new EventEmitter<string>();*/

    /*nextStageId: string;*/ /* will hold the id of the next stage */
    /*nextStageDelegate: string;*/ /* in stage5 verbal expression of the user's choice for the next step */

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

    /*additional form controls will be added dynamically according to the stage*/
    stageFormDefinition = {
        comment: ''
    };
    uploadedFile: File;

    @Input() currentStage: any;
    @Input() currentProject: Project;
    currentPOI: POI;
    stageDescription: StageDescription;  /*contains the name of the delegate field and the list of the extra fields descriptions*/
    stageExtraFieldsList: string[] = []; /*contains the names of the extra fields inside the current stage class*/
                                         /*they will be used as formControlNames and also as a way
                                           to access the corresponding properties of the Stage object*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {}

    ngOnInit() {
        /*console.log(`showStage ${this.stageDescription.id} is ${this.showStage}`);*/
        this.checkIfSubmitted();
        this.checkIfApproved();
        this.stageTitle = stagesMap[this.stageDescription.id];
    }

    checkIfSubmitted() {
        console.log(`hasReturned is ${this.hasReturnedToPrevious}`);
        this.wasSubmitted = ( (!isNullOrUndefined(this.currentStage) &&
                              !isNullOrUndefined(this.currentStage.date)) &&
                              (this.hasReturnedToPrevious !== 1) );
        if ( !this.wasSubmitted && (this.hasReturnedToPrevious !== 2) && ( this.authService.getUserRole() !== 'ROLE_USER' )) {
            this.stageForm = this.fb.group(this.stageFormDefinition);
            if ( !isNullOrUndefined(this.currentStage['comment']) ) {
                this.stageForm.get('comment').setValue(this.currentStage['comment']);
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
                      ( this.stageDescription.id === '6' || this.stageDescription.id === '11' ) ) ) {

                    this.wasApproved = 'Εγκρίθηκε';
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

    /*creates extra formControls according to the extra fields list*/
    createExtraFields() {
        if (this.stageForm) {
            for (const newControl of this.stageExtraFieldsList) {
                this.stageForm.addControl(newControl, new FormControl());
            }
            this.addStageInfoToForm();
        }
    }

    addStageInfoToForm() {
        if (!isNullOrUndefined(this.currentStage) &&
            !isNullOrUndefined(this.currentStage.date)) {

            for (const newControl of this.stageExtraFieldsList) {
                this.stageForm.get(newControl).setValue(this.currentStage[newControl]);
            }
        }
    }

    getAttachmentInput(newFile: File) {
        this.uploadedFile = newFile;
        console.log('this.uploadedFile is : ', this.uploadedFile);
    }

    approveRequest( approved: boolean ) {
        this.currentStage['approved'] = approved;
        if (approved) {
             if (this.areAllCheckBoxesTrue() ) {
                 this.submitForm();
             } else {
                 this.stageFormError = 'Πρέπει να έχουν γίνει όλοι οι έλεγχοι για να προχωρήσει το αίτημα.';
             }
        } else {
            /*this.nextStageId = this.stageDescription.id;*/
            this.submitForm();
        }
    }

    areAllCheckBoxesTrue() {
        for ( let i = 0; i < this.stageExtraFieldsList.length; i++ ) {
            if (this.stageDescription.stageFields[i].type === 'checkbox' && !this.stageForm.get(this.stageExtraFieldsList[i]).value ) {

                return false;
            }
        }
        return true;
    }

    /* NOT USED ANYMORE - RESTORE IF STAGE 5 IS RESTORED */
    /*forwardRequest( nextStage: string, showModal: boolean ) {
        this.nextStageId = nextStage;
        console.log('nextStage is', this.nextStageId);
        if (nextStage === '5a') {
            this.nextStageDelegate = 'στον Γενικό Διευθυντή';
        } else {
            this.nextStageDelegate = 'στο Διοικητικό Συμβούλιο';
        }
        if (showModal) {
            UIkit.modal('#nextStage').show();
        } else {
            this.currentStage['approved'] = true;
            this.submitForm();
        }
    }

    getNextStageDelegate() {
        return this.nextStageDelegate;
    }

    onConfirm(event: any) {
        UIkit.modal('#nextStage').hide();
        this.currentStage['approved'] = true;
        this.submitForm();
    }*/

    goBackOneStage() {
        this.stageFormError = '';
        if ( !this.stageForm.get('comment').value ) {
            this.stageFormError = 'Είναι υποχρεωτικό να γράψετε ένα σχόλιο για την επιλογή σας.';
        } else {
            this.emitGoBack.emit(true);
            this.submitForm();
        }
    }

    submitForm() {
        this.stageFormError = '';
        if (this.stageForm && this.stageForm.valid && this.delegateCanEdit() ) {
        /*if (this.stageForm && this.stageForm.valid ) {*/
            if ( (this.stageDescription.id === '6' ||
                  this.stageDescription.id === '11' ||
                  (this.stageDescription.id === '7' &&
                   this.currentStage['approved']) ) &&
                !this.uploadedFile ) {

                this.stageFormError = 'Η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else {
                /* NOT USED ANYMORE - RESTORE IF STAGE 5 IS RESTORED */
                /*if (!this.nextStageId) {
                    if ( this.stageDescription.id === '13' ) {
                        this.nextStageId = '13';
                    } else if (this.stageDescription.id !== '5a' && this.stageDescription.id !== '5b') {
                        this.nextStageId = (+this.stageDescription.id + 1).toString();
                    } else {
                        this.nextStageId = '6';
                    }
                    console.log('nextStage is', this.nextStageId);
                }
                this.emitNextStage.emit(this.nextStageId);*/

                this.currentStage['user'] = this.createUser();
                this.currentStage['date'] = Date.now().toString();
                for ( const stageField of this.stageExtraFieldsList ) {
                    this.currentStage[stageField] = this.stageForm.get(stageField).value;
                }
                this.currentStage['comment'] = this.stageForm.get('comment').value;
                if (this.uploadedFile) {
                    this.currentStage['attachment'] = this.createAttachment();
                }

                console.log(this.currentStage);
                this.checkIfSubmitted();
                this.checkIfApproved();
                this.emitStage.emit(this.currentStage);
            }
        }/* else {
            UIkit.modal.alert('Φαίνεται ότι δεν έχετε δικαίωμα να εγκρίνετε ή να απορρίψετε αυτό το στάδιο.');
        }*/
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
    templateUrl: './stages-components.html'
})
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον επιστημονικό υπεύθυνο';
        this.stageDescription = Stage2Desc;
        this.currentPOI = this.currentProject.scientificCoordinator;
        super.ngOnInit();
        this.stageExtraFieldsList = ['checkNecessity', 'checkFeasibility'];
        this.createExtraFields();
    }

}


@Component ({
    selector: 'stage3-component',
    templateUrl: './stages-components.html'
})
export class Stage3Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον χειριστή του προγράμματος';
        this.stageDescription = Stage3Desc;
        this.currentPOI = this.findCurrentPOI(this.currentProject.operator);
        super.ngOnInit();
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable', 'loan', 'loanSource'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage4-component',
    templateUrl: './stages-components.html'
})
export class Stage4Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage4Desc;
        this.currentPOI = this.currentProject.institute.organization.POI;
        super.ngOnInit();
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
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
        this.delegatePositionInParagraph = 'από τον Διευθυντή του Ινστιτούτου';
        this.stageDescription = Stage5Desc;
        this.currentPOI = this.currentProject.institute.director;
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage5a-component',
    templateUrl: './stages-components.html'
})
export class Stage5aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον Διατάκτη';
        this.stageDescription = Stage5aDesc;
        this.currentPOI = this.currentProject.institute.organization.director;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage5b-component',
    templateUrl: './stages-components.html'
})
export class Stage5bComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από το Διοικητικό Συμβούλιο';
        this.stageDescription = Stage5bDesc;
        this.currentPOI = this.currentProject.institute.organization.dioikitikoSumvoulio;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage6-component',
    templateUrl: './stages-components.html'
})
export class Stage6Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από την ΔΙΑΥΓΕΙΑ';
        this.stageDescription = Stage6Desc;
        this.currentPOI = this.currentProject.institute.diaugeia;
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage7-component',
    templateUrl: './stages-components.html'
})
export class Stage7Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage7Desc;
        this.currentPOI = this.findCurrentPOI(this.currentProject.operator);
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage8-component',
    templateUrl: './stages-components.html'
})
export class Stage8Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον Διευθυντή Λογιστηρίου';
        this.stageDescription = Stage8Desc;
        this.currentPOI = this.currentProject.institute.accountingDirector;
        super.ngOnInit();
        this.stageExtraFieldsList = ['checkRegularity', 'checkLegality'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage9-component',
    templateUrl: './stages-components.html'
})
export class Stage9Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage9Desc;
        this.currentPOI = this.currentProject.institute.organization.POI;
        super.ngOnInit();
        this.stageExtraFieldsList = ['checkRegularity', 'checkLegality'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage10-component',
    templateUrl: './stages-components.html'
})
export class Stage10Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον Διατάκτη';
        this.stageDescription = Stage10Desc;
        this.currentPOI = this.currentProject.institute.organization.director;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage11-component',
    templateUrl: './stages-components.html'
})
export class Stage11Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από την ΔΙΑΥΓΕΙΑ';
        this.stageDescription = Stage11Desc;
        this.currentPOI = this.currentProject.institute.diaugeia;
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage12-component',
    templateUrl: './stages-components.html'
})
export class Stage12Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage12Desc;
        this.currentPOI = this.currentProject.institute.accountingRegistration;
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage13-component',
    templateUrl: './stages-components.html'
})
export class Stage13Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage13Desc;
        this.currentPOI = this.currentProject.institute.accountingPayment;
        super.ngOnInit();
    }
}
