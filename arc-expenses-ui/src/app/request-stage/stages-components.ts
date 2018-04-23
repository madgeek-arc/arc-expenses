import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {Attachment, Delegate, Stage2, Stage3} from '../domain/operation';
import {commentDesc, Stage2Desc, Stage3Desc, StageDescription, StageFieldDescription} from '../domain/stageDescriptions';
import {DatePipe} from '@angular/common';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {

    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();
    @Input() wasSubmitted: boolean;

    stageForm: FormGroup;

    /*additional controls will be added dynamically according to the stage*/
    stageFormDefinition = {
        comment: [''],
        attachment: ['', Validators.required]
    };

    @Input() currentStage: any;
    stageDescription: StageDescription; /*contains the name of the delegate field and the list of the extra fields descriptions*/
    stageExtraFieldsList: string[] = []; /*contains the names of the extra fields inside the current stage class*/
                                         /*they will be used as formControlNames and also to access the corresponding values*/

    commentFieldDesc: StageFieldDescription = commentDesc; /*a description for the comments field*/

    datePipe = new DatePipe('en-us');

    constructor(private fb: FormBuilder) {}

    ngOnInit() {
        this.stageForm = this.fb.group(this.stageFormDefinition);
        this.getAttachmentInput('attachment url');
    }

    createExtraFields() {
        if (this.stageForm) {
            for (const newControl of this.stageExtraFieldsList) {
                this.stageForm.addControl(newControl, new FormControl());
            }
        }
    }

    getAttachmentInput(newAttachment: string) {
        this.stageForm.get('attachment').setValue(newAttachment);
    }

    approveRequest( wasApproved: boolean ) {
        this.currentStage['approved'] = wasApproved;
        this.submitForm();
    }

    submitForm() {
        if (this.stageForm && this.stageForm.valid) {
            this.currentStage[this.stageDescription.delegateField] = this.createDelegate();
            this.currentStage['date'] = this.getCurrentDateString();
            for (const controlName of this.stageExtraFieldsList) {
                this.currentStage[controlName] = this.stageForm.get(controlName).value;
            }
            this.currentStage['comment'] = this.stageForm.get('comment').value;
            this.currentStage['attachment'] = this.createAttachment();
            console.log(this.currentStage);
            this.emitStage.emit(this.currentStage);
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
        const tempAttahment: Attachment = new Attachment();
        tempAttahment.filename = '';
        tempAttahment.mimetype = '';
        tempAttahment.size = 0;
        tempAttahment.url = this.stageForm.get('attachment').value;
        return tempAttahment;
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
        <hr class="uk-divider-icon">
        <h5 class="uk-h4">Stage2</h5>
        <div>Εγκρίθηκε από τον επιστημονικό υπεύθυνο ({{ currentStage[stageDescription.delegateField]['firstname'] }}
             {{ currentStage[stageDescription.delegateField]['lastname'] }}) την {{ currentStage['date'] }}</div>
        <div *ngIf="currentStage['comment']"><span class="uk-text-bold">Σχόλια: </span><span> {{ currentStage['comment'] }} </span></div>
        <div><a class="uk-link" href="#">Πατήστε εδώ για να κατεβάσετε τα σχετικά αρχεία</a></div>
    </div>
    <div *ngIf="!wasSubmitted" [formGroup]="stageForm" class="uk-form-controls">
        <hr>
        <h5 class="uk-h4">Stage2</h5>
        <app-stage-form [description]="commentFieldDesc">
            <textarea formControlName="comment" class="uk-form-controls uk-textarea"></textarea>
        </app-stage-form>
        <div>
            <div class="uk-margin-top uk-margin-bottom" style="text-align: center; padding: 20px; border:4px dashed lightgray;">
                <div *ngIf="!stageForm.get('attachment').value">
                    Επισυνάψτε το αρχείο σας ρίχνοντάς το εδώ ή πατώντας <a href="#">εδώ</a>
                </div>
                <div><a href="#" title="Click to choose another file">{{ stageForm.get('attachment').value }}</a></div>
                <input formControlName="attachment" type="text" hidden>
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
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        super.ngOnInit();
        if (!this.wasSubmitted) { this.currentStage = new Stage2(); }
        this.stageDescription = Stage2Desc;
    }

}


@Component ({
    selector: 'stage3-component',
    template: `
<div class="uk-width-1-1 uk-margin-bottom">
    <div *ngIf="wasSubmitted && currentStage" class="uk-form-controls">
        <hr class="uk-divider-icon">
        <h5>Stage3</h5>
        <div>Εγκρίθηκε από τον χειριστή του προγράμματος ({{ currentStage[stageDescription.delegateField]['firstname'] }}
            {{ currentStage[stageDescription.delegateField]['lastname'] }}) την {{ currentStage['date'] }}</div>
        <div *ngFor="let desc of stageDescription.stageFields; let controlIndex = index">
            <app-stage-form [description]="desc">
                <input class="uk-form-controls uk-checkbox"
                       type="checkbox" [checked]="currentStage[ stageExtraFieldsList[controlIndex] ]" disabled>
            </app-stage-form>
        </div>
        <div *ngIf="currentStage['comment']"><span class="uk-text-bold">Σχόλια: </span><span> {{ currentStage['comment'] }} </span></div>
        <div><a class="uk-link" href="#">Πατήστε εδώ για να κατεβάσετε τα σχετικά αρχεία</a></div>
    </div>
    <div *ngIf="!wasSubmitted" [formGroup]="stageForm" class="uk-form-controls">
        <hr>
        <h5>Stage3</h5>
        <div *ngFor="let desc of stageDescription.stageFields; let controlIndex = index">
            <app-stage-form [description]="desc">
                <input formControlName="{{stageExtraFieldsList[controlIndex]}}" class="uk-form-controls uk-checkbox" type="checkbox">
            </app-stage-form>
        </div>
        <app-stage-form [description]="commentFieldDesc">
            <textarea formControlName="comment" class="uk-form-controls uk-textarea" rows="3"></textarea>
        </app-stage-form>
        <div>
            <div class="uk-margin-top uk-margin-bottom" style="text-align: center; padding: 20px; border:4px dashed lightgray;">
                <div *ngIf="!stageForm.get('attachment').value">
                    Επισυνάψτε το αρχείο σας ρίχνοντάς το εδώ ή πατώντας <a href="#">εδώ</a>
                </div>
                <div><a href="#" title="Click to choose another file">{{ stageForm.get('attachment').value }}</a></div>
                <input formControlName="attachment" type="text" hidden>
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
        super.ngOnInit();
        if (!this.wasSubmitted) { this.currentStage = new Stage3(); }
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required]
        };*/
        super.ngOnInit();
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required]
        };*/
        super.ngOnInit();
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required],
            checkRegularity: ['', Validators.required],
            checkLegality: ['', Validators.required]
        };*/
        super.ngOnInit();
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required],
            checkRegularity: ['', Validators.required],
            checkLegality: ['', Validators.required]
        };*/
        super.ngOnInit();
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required]
        };*/
        super.ngOnInit();
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
        /*this.stageFormDefinition = {
            approved: ['', Validators.required]
        };*/
        super.ngOnInit();
    }
}
