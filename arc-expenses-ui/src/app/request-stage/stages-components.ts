import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {FormBuilder, FormControl, FormGroup } from '@angular/forms';
import {
    Attachment, Delegate, Stage2, Stage3, Stage3b, Stage3a, Stage4, Stage5,
    Stage6, Stage7, Stage8, Stage9, Stage10
} from '../domain/operation';
import {
    commentDesc, Stage10Desc, Stage2Desc, Stage3aDesc, Stage3bDesc, Stage3Desc, Stage4Desc, Stage5Desc, Stage6Desc,
    Stage7Desc, Stage8Desc, Stage9Desc, StageDescription, StageFieldDescription
} from '../domain/stageDescriptions';
import {DatePipe} from '@angular/common';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {

    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();
    @Input() showStage: boolean;
    wasSubmitted: boolean;
    wasApproved: boolean;

    /*  phrase mentioning the delegate's position.
        It is used inside the paragraph containing the results of the stage*/
    delegatePositionInParagraph: string;

    stageForm: FormGroup;
    stageTitle: string;

    /*additional controls will be added dynamically according to the stage*/
    stageFormDefinition = {
        comment: ['']
    };
    uploadedFile: File;

    @Input() currentStage: any;
    stageDescription: StageDescription;  /*contains the name of the delegate field and the list of the extra fields descriptions*/
    stageExtraFieldsList: string[] = []; /*contains the names of the extra fields inside the current stage class*/
                                         /*they will be used as formControlNames and also to access the corresponding values*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('el');

    constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {}

    ngOnInit() {
        this.checkIfSubmitted();
        this.checkIfApproved();
    }

    checkIfSubmitted() {
        this.wasSubmitted = ( this.currentStage && this.currentStage.date );
        if (!this.wasSubmitted) {
            this.stageForm = this.fb.group(this.stageFormDefinition);
        }
    }

    checkIfApproved() {
        this.wasApproved = ( this.currentStage && this.currentStage['approved'] );

        if (this.stageTitle && this.stageTitle === 'Stage 6') { this.wasApproved = true; }
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
        /*run script to upload file*/
        /*this.uploadedFile = event.target.files[0];*/
        this.uploadedFile = newFile;
        console.log('this.uploadedFile is : ', this.uploadedFile);
    }

    approveRequest( approved: boolean ) {
        this.currentStage['approved'] = approved;
        this.submitForm();
    }

    submitForm() {
        if (this.stageForm && this.stageForm.valid) {
            /*PROBABLY ALREADY FILLED !*/
            this.currentStage[this.stageDescription.delegateField] = this.createDelegate();
            this.currentStage['date'] = this.getCurrentDateString();
            for (const controlName of this.stageExtraFieldsList) {
                this.currentStage[controlName] = this.stageForm.get(controlName).value;
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
        this.stageTitle = 'Stage 2';
        this.delegatePositionInParagraph = 'από τον επιστημονικό υπεύθυνο';
        super.ngOnInit();
        this.stageDescription = Stage2Desc;
    }

}


@Component ({
    selector: 'stage3-component',
    templateUrl: './stages-components.html'
})
export class Stage3Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 3';
        this.delegatePositionInParagraph = 'από τον χειριστή του προγράμματος';
        super.ngOnInit();
        this.stageDescription = Stage3Desc;
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage3a-component',
    templateUrl: './stages-components.html'
})
export class Stage3aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 3a';
        this.delegatePositionInParagraph = 'από τον διευθυντή του Οργανισμού';
        super.ngOnInit();
        this.stageDescription = Stage3aDesc;
    }
}

@Component ({
    selector: 'stage3b-component',
    templateUrl: './stages-components.html'
})
export class Stage3bComponent extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 3b';
        this.delegatePositionInParagraph = 'από το Διοικητικό Συμβούλιο';
        super.ngOnInit();
        this.stageDescription = Stage3bDesc;
    }
}

@Component ({
    selector: 'stage4-component',
    templateUrl: './stages-components.html'
})
export class Stage4Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 4';
        this.delegatePositionInParagraph = '';
        super.ngOnInit();
        this.stageDescription = Stage4Desc;
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage5-component',
    templateUrl: './stages-components.html'
})
export class Stage5Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 5';
        this.delegatePositionInParagraph = 'από τον διευθυντή του Ινστιτούτου';
        super.ngOnInit();
        this.stageDescription = Stage5Desc;
    }
}

@Component ({
    selector: 'stage6-component',
    templateUrl: './stages-components.html'
})
export class Stage6Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 6';
        this.delegatePositionInParagraph = 'από την ΔΙΑΥΓΕΙΑ';
        super.ngOnInit();
        this.stageDescription = Stage6Desc;
    }
}


@Component ({
    selector: 'stage7-component',
    templateUrl: './stages-components.html'
})
export class Stage7Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 7';
        this.delegatePositionInParagraph = 'από τον διευθυντή λογιστηρίου';
        super.ngOnInit();
        this.stageDescription = Stage7Desc;
        this.stageExtraFieldsList = ['checkRegularity', 'checkLegality'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage8-component',
    templateUrl: './stages-components.html'
})
export class Stage8Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 8';
        this.delegatePositionInParagraph = '';
        super.ngOnInit();
        this.stageDescription = Stage8Desc;
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
        this.stageTitle = 'Stage 9';
        this.delegatePositionInParagraph = '';
        super.ngOnInit();
        this.stageDescription = Stage9Desc;
    }
}

@Component ({
    selector: 'stage10-component',
    templateUrl: './stages-components.html'
})
export class Stage10Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 10';
        this.delegatePositionInParagraph = '';
        super.ngOnInit();
        this.stageDescription = Stage10Desc;
    }
}
