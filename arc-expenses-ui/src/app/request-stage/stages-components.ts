import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {
    Attachment, Delegate, Stage2, Stage3, Stage3b, Stage3a, Stage4, Stage5,
    Stage6, Stage7, Stage8, Stage9, Stage10
} from '../domain/operation';
import {
    commentDesc, Stage10Desc, Stage2Desc, Stage3aDesc, Stage3bDesc, Stage3Desc, Stage4Desc, Stage5Desc, Stage6Desc,
    Stage7Desc, Stage8Desc, Stage9Desc, StageDescription, StageFieldDescription
} from '../domain/stageDescriptions';
import {DatePipe} from '@angular/common';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {

    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();
    @Input() wasSubmitted: boolean;
    wasApproved: boolean;

    stageForm: FormGroup;
    stageTitle: string;

    /*additional controls will be added dynamically according to the stage*/
    stageFormDefinition = {
        comment: ['']
    };
    uploadedFile: File;

    @Input() currentStage: any;
    stageDescription: StageDescription; /*contains the name of the delegate field and the list of the extra fields descriptions*/
    stageExtraFieldsList: string[] = []; /*contains the names of the extra fields inside the current stage class*/
                                         /*they will be used as formControlNames and also to access the corresponding values*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('en-us');

    constructor(private fb: FormBuilder) {}

    ngOnInit() {
        this.stageForm = this.fb.group(this.stageFormDefinition);
        if (this.currentStage && this.currentStage['approved']) {
            this.wasApproved = this.currentStage['approved'];
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

    getAttachmentInput(event: any) {
        /*run script to upload file*/
        this.uploadedFile = event.target.files[0];
        console.log('this.uploadedFile is : ', this.uploadedFile);
    }

    getDroppedFile(event: any) {
        event.preventDefault();
        console.log(event.dataTransfer.files[0]);
        /*run script to upload file*/
        this.uploadedFile = <File>event.dataTransfer.files[0];
        console.log('this.droppedFile is : ', this.uploadedFile);
    }

    allowDrop(event: any) {
        event.preventDefault();
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
            this.currentStage['attachment'] = this.createAttachment();
            console.log(this.currentStage);
            /*call api and update request*/
            this.emitStage.emit(this.currentStage); /*or false according to the api response*/
        }
    }

    createDelegate(): Delegate {
        const tempDelegate: Delegate = new Delegate();
        tempDelegate.email = '';
        tempDelegate.firstname = '';
        tempDelegate.lastname = '';
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
    template: `
<div class="uk-width-1-1 uk-margin-bottom">
    <div *ngIf="wasSubmitted && currentStage" class="uk-form-controls">
        <hr>
        <h5>{{ stageTitle }}</h5>
        <div>
            <span *ngIf="wasApproved">Εγκρίθηκε</span>
            <span *ngIf="!wasApproved">Απορρίφθηκε</span>
            από τον επιστημονικό υπεύθυνο ({{ currentStage[stageDescription.delegateField]['firstname'] }}
            {{ currentStage[stageDescription.delegateField]['lastname'] }}) την {{ currentStage['date'] }}
        </div>
        <div *ngIf="currentStage['comment']"><span class="uk-text-bold">
            Σχόλια: </span><span> {{ currentStage['comment'] }} </span>
        </div>
        <div><a class="uk-link"
                href="{{ currentStage['attachment'] ? currentStage['attachment']['url'] : '#' }}">
            Πατήστε εδώ για να κατεβάσετε τα σχετικά αρχεία</a>
        </div>
    </div>
    <div *ngIf="!wasSubmitted" [formGroup]="stageForm" class="uk-form-controls">
        <hr>
        <h5>{{ stageTitle }}</h5>
        <app-stage-form [description]="commentFieldDesc">
            <textarea formControlName="comment" class="uk-form-controls uk-textarea"></textarea>
        </app-stage-form>
        <div data-tooltip title="Επιλέξτε αρχείο" (drop)="getDroppedFile($event)" (dragover)="allowDrop($event)">
            <div class="uk-link uk-placeholder uk-text-center uk-margin-top uk-width-1-1" uk-form-custom>
                <i class="uk-icon-cloud-upload uk-icon-medium uk-text-muted uk-margin-small-right"></i>
                <input type="file" name="selectedFile" (change)="getAttachmentInput($event)">
                <span class="uk-link">Επισυνάψτε το αρχείο σας ρίχνοντάς το εδώ ή πατώντας εδώ</span>
                <div><span class="uk-text-bold">Επιλεγμένο αρχείο: </span>
                    {{uploadedFile ? uploadedFile.name : "επιλέξτε αρχείο"}}</div>
            </div>
        </div>
        <div>
            <button class="uk-button uk-button-primary" (click)="approveRequest(true)">Εγκρίνεται</button>
            <button class="uk-button uk-button-primary" (click)="approveRequest(false)">Απορρίπτεται</button>
        </div>
        <div><span class="uk-text-small">
            ημερομηνία κατάθεσης: {{ getCurrentDateString() }}</span>
        </div>
    </div>
</div>
`
})
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 2';
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage2(); }
        this.stageDescription = Stage2Desc;
    }

}


@Component ({
    selector: 'stage3-component',
    template: `
<div class="uk-width-1-1 uk-margin-bottom">
    <div *ngIf="wasSubmitted && currentStage" class="uk-form-controls">
        <hr>
        <h5>{{ stageTitle }}</h5>
        <div>
            <span *ngIf="wasApproved">Εγκρίθηκε</span>
            <span *ngIf="!wasApproved">Απορρίφθηκε</span>
            από τον χειριστή του προγράμματος ({{ currentStage[stageDescription.delegateField]['firstname'] }}
            {{ currentStage[stageDescription.delegateField]['lastname'] }}) την {{ currentStage['date'] }}
        </div>
        <div *ngFor="let desc of stageDescription.stageFields; let controlIndex = index">
            <app-stage-form [description]="desc">
                <input class="uk-form-controls uk-checkbox"
                       type="checkbox" [checked]="currentStage[ stageExtraFieldsList[controlIndex] ]" disabled>
            </app-stage-form>
        </div>
        <div *ngIf="currentStage['comment']"><span class="uk-text-bold">Σχόλια: </span><span> {{ currentStage['comment'] }} </span></div>
        <div><a class="uk-link" href="{{ currentStage['attachment'] ? currentStage['attachment']['url'] : '#' }}">
            Πατήστε εδώ για να κατεβάσετε τα σχετικά αρχεία</a></div>
    </div>
    <div *ngIf="!wasSubmitted" [formGroup]="stageForm" class="uk-form-controls">
        <hr>
        <h5>{{ stageTitle }}</h5>
        <div *ngFor="let desc of stageDescription.stageFields; let controlIndex = index">
            <app-stage-form [description]="desc">
                <input formControlName="{{stageExtraFieldsList[controlIndex]}}" class="uk-form-controls uk-checkbox" type="checkbox">
            </app-stage-form>
        </div>
        <app-stage-form [description]="commentFieldDesc">
            <textarea formControlName="comment" class="uk-form-controls uk-textarea" rows="2"></textarea>
        </app-stage-form>
        <div data-tooltip title="Επιλέξτε αρχείο" (drop)="getDroppedFile($event)" (dragover)="allowDrop($event)">
            <div class="uk-link uk-placeholder uk-text-center uk-margin-top uk-width-1-1" uk-form-custom>
                <i class="uk-icon-cloud-upload uk-icon-medium uk-text-muted uk-margin-small-right"></i>
                <input type="file" name="selectedFile" (change)="getAttachmentInput($event)">
                <span class="uk-link">Επισυνάψτε το αρχείο σας ρίχνοντάς το εδώ ή πατώντας εδώ</span>
                <div><span class="uk-text-bold">Επιλεγμένο αρχείο: </span>
                    {{uploadedFile ? uploadedFile.name : "επιλέξτε αρχείο"}}</div>
            </div>
        </div>
        <div>
            <button class="uk-button uk-button-primary" (click)="approveRequest(true)">Εγκρίνεται</button>
            <button class="uk-button uk-button-primary" (click)="approveRequest(false)">Απορρίπτεται</button>
        </div>
        <div><span class="uk-text-small">Το έγγραφο θα κατατεθεί με ημερομηνία {{ getCurrentDateString() }}</span></div>
    </div>
</div>
`
})
export class Stage3Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageTitle = 'Stage 3';
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage3(); }
        this.stageDescription = Stage3Desc;
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage3a-component',
    template: `
        <div>Stage3a</div>
    `
})
export class Stage3aComponent extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage3a(); }
        this.stageDescription = Stage3aDesc;
    }
}

@Component ({
    selector: 'stage3b-component',
    template: `
        <div>Stage3b</div>
    `
})
export class Stage3bComponent extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage3b(); }
        this.stageDescription = Stage3bDesc;
    }
}

@Component ({
    selector: 'stage4-component',
    template: `
        <div>Stage4</div>
    `
})
export class Stage4Component extends StageComponent implements OnInit {

    ngOnInit () {
        /*this.stageFormDefinition = {
            approved: ['', Validators.required],
            analiftheiYpoxrewsi: ['', Validators.required],
            fundsAvailable: ['', Validators.required]
        };*/
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage4(); }
        this.stageDescription = Stage4Desc;
        this.stageExtraFieldsList = ['analiftheiYpoxrewsi', 'fundsAvailable'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage5-component',
    template: `
        <div>Stage5</div>
    `
})
export class Stage5Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage5(); }
        this.stageDescription = Stage5Desc;
    }
}

@Component ({
    selector: 'stage6-component',
    template: `
        <div>Stage6</div>
        <!--ONLY one button for submit with name: Υποβολή-->
    `
})
export class Stage6Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        /*NO APPROVE FIELD !!*/
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage6(); }
        this.stageDescription = Stage6Desc;
    }
}


@Component ({
    selector: 'stage7-component',
    template: `
        <div>Stage7</div>
    `
})
export class Stage7Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage7(); }
        this.stageDescription = Stage7Desc;
        this.stageExtraFieldsList = ['checkRegularity', 'checkLegality'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage8-component',
    template: `
        <div>Stage8</div>
    `
})
export class Stage8Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage8(); }
        this.stageDescription = Stage8Desc;
        this.stageExtraFieldsList = ['checkRegularity', 'checkLegality'];
        this.createExtraFields();
    }
}

@Component ({
    selector: 'stage9-component',
    template: `
        <div>Stage9</div>
    `
})
export class Stage9Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage9(); }
        this.stageDescription = Stage9Desc;
    }
}

@Component ({
    selector: 'stage10-component',
    template: `
        <div>Stage10</div>
    `
})
export class Stage10Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted || !this.currentStage) { this.currentStage = new Stage10(); }
        this.stageDescription = Stage10Desc;
    }
}
