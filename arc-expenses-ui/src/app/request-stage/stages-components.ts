import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import { Attachment, Delegate } from '../domain/operation';
import {
    commentDesc, Stage10Desc, Stage2Desc, Stage5aDesc, Stage5bDesc, Stage3Desc, Stage4Desc, Stage5Desc, Stage6Desc,
    Stage7Desc, Stage8Desc, Stage9Desc, StageDescription, StageFieldDescription, Stage12Desc, stagesMap, Stage11Desc
} from '../domain/stageDescriptions';
import {DatePipe} from '@angular/common';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';

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

    /* sends the next stage id back to the parent component*/
    @Output() emitNextStage: EventEmitter<string> = new EventEmitter<string>();

    nextStageId: string; /* will hold the id of the next stage */
    nextStageDelegate: string; /* in stage5 verbal expression of the user's choice for the next step */

    /* input variable that controls if the current stage template should be displayed  */
    @Input() showStage: boolean;

    /* if true, the results of the stage will be shown
     * otherwise a form will be shown so that the user can update the request */
    wasSubmitted: boolean;

    /* if the stage was submitted in the past, this variable will control the displayed
     * text according to the submitted stage's status [approved/rejected] */
    wasApproved: boolean;

    /*  phrase mentioning the delegate's position.
        It is used to describe the results of the stage*/
    delegatePositionInParagraph: string;

    stageForm: FormGroup;
    stageTitle: string;

    /*additional form controls will be added dynamically according to the stage*/
    stageFormDefinition = {
        comment: ['']
    };
    uploadedFile: File;

    @Input() currentStage: any;
    stageDescription: StageDescription;  /*contains the name of the delegate field and the list of the extra fields descriptions*/
    stageExtraFieldsList: string[] = []; /*contains the names of the extra fields inside the current stage class*/
                                         /*they will be used as formControlNames and also as a way
                                           to access the corresponding properties of the Stage object*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {}

    ngOnInit() {
        this.checkIfSubmitted();
        this.checkIfApproved();
        this.stageTitle = stagesMap[this.stageDescription.id];
    }

    checkIfSubmitted() {
        this.wasSubmitted = ( this.currentStage && this.currentStage.date );
        if (!this.wasSubmitted) {
            this.stageForm = this.fb.group(this.stageFormDefinition);
        }
    }

    checkIfApproved() {
        this.wasApproved = ( this.currentStage && this.currentStage['approved'] );

        if (this.stageDescription && (this.stageDescription.id === '6' || this.stageDescription.id === '11') ) {
            this.wasApproved = true;
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
            this.nextStageId = this.stageDescription.id;
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

    forwardRequest( nextStage: string, showModal: boolean ) {
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
    }

    submitForm() {
        this.stageFormError = '';
        if (this.stageForm && this.stageForm.valid) {
            if ( (this.stageDescription.id === '6' ||
                  this.stageDescription.id === '11' ||
                  (this.stageDescription.id === '7' &&
                   this.currentStage['approved']) ) &&
                !this.uploadedFile ) {

                this.stageFormError = 'Η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else if (this.stageDescription.id === '10' &&
                       (!this.stageForm.get('accountCode').value || !this.stageForm.get('accountDescription').value)) {
                this.stageFormError = 'Παρακαλώ συμπληρώστε τα υποχρεωτικά πεδία.';
            } else {
                if (!this.nextStageId) {
                    if ( this.stageDescription.id === '12' ) {
                        this.nextStageId = '12';
                    } else if (this.stageDescription.id !== '5a' && this.stageDescription.id !== '5b') {
                        this.nextStageId = (+this.stageDescription.id + 1).toString();
                    } else {
                        this.nextStageId = '6';
                    }
                    console.log('nextStage is', this.nextStageId);
                }
                this.emitNextStage.emit(this.nextStageId);

                this.currentStage[this.stageDescription.delegateField] = this.createDelegate();
                this.currentStage['date'] = Date.now().toString();
                for ( const stageField of this.stageExtraFieldsList ) {
                    this.currentStage[stageField] = this.stageForm.get(stageField).value;
                }
                this.currentStage['comment'] = this.stageForm.get('comment').value;
                if (this.uploadedFile) {
                    this.currentStage['attachment'] = this.createAttachment();
                }

                console.log(this.currentStage);
                /*call api and update request*/
                this.checkIfSubmitted();
                this.checkIfApproved();
                this.emitStage.emit(this.currentStage);
            }
        }
    }

    createDelegate(): Delegate {
        const tempDelegate: Delegate = new Delegate();
        tempDelegate.email = this.authService.getUserEmail();
        tempDelegate.firstname = this.authService.getUserFirstName();
        tempDelegate.lastname = this.authService.getUserLastName();
        tempDelegate.hidden = false; /*WILL THE USER BE GIVEN THIS CHOICE?*/
        return tempDelegate;
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
        super.ngOnInit();
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
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
        super.ngOnInit();
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
    }
}

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
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage5a-component',
    templateUrl: './stages-components.html'
})
export class Stage5aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.delegatePositionInParagraph = 'από τον Διευθυντή του Οργανισμού';
        this.stageDescription = Stage5aDesc;
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
        this.delegatePositionInParagraph = '';
        this.stageDescription = Stage10Desc;
        super.ngOnInit();
        this.stageExtraFieldsList = ['accountCode', 'accountDescription'];
        this.createExtraFields();
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
        super.ngOnInit();
    }
}
