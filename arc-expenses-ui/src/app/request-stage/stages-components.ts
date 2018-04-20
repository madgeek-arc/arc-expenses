import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Attachment, Delegate } from '../domain/operation';

@Component ({
    selector: 'stage-component',
    template: ``
})
export class StageComponent implements OnInit {

    stageForm: FormGroup;
    stageFormDefinition = {};

    @Input() currentDelegate: Delegate;
    date: string;
    comment: string;
    attachment: Attachment;

    constructor(private fb: FormBuilder) {}

    ngOnInit() {
        this.stageForm = this.fb.group(this.stageFormDefinition);
    }
}

@Component ({
    selector: 'stage2-component',
    template: `
        <div>Stage2</div>
    `
})
export class Stage2Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
        super.ngOnInit();
    }
}


@Component ({
    selector: 'stage3-component',
    template: `
        <div>Stage3</div>
    `
})
export class Stage3Component extends StageComponent implements OnInit {

    ngOnInit () {
        this.stageFormDefinition = {
            analiftheiYpoxrewsi: ['', Validators.required],
            fundsAvailable: ['', Validators.required],
            approved: ['', Validators.required]
        };
        super.ngOnInit();
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
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required],
            analiftheiYpoxrewsi: ['', Validators.required],
            fundsAvailable: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
        super.ngOnInit();
    }
}

@Component ({
    selector: 'stage6-component',
    template: `
        <div>Stage6</div>
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
        this.stageFormDefinition = {
            approved: ['', Validators.required],
            checkRegularity: ['', Validators.required],
            checkLegality: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required],
            checkRegularity: ['', Validators.required],
            checkLegality: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
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
        this.stageFormDefinition = {
            approved: ['', Validators.required]
        };
        super.ngOnInit();
    }
}
