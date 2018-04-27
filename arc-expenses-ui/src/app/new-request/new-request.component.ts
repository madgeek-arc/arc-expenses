import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Project, Request, Stage1} from '../domain/operation';

@Component({
    selector: 'app-new-request',
    templateUrl: './new-request.component.html',
    styleUrls: ['./new-request.component.scss']
})
export class NewRequestComponent implements OnInit {

    newRequestForm: FormGroup;

    uploadedFile: File;

    request: Request;

    institutes = ['ILSP', 'IMSI', 'ISI', 'SPU', 'PPA'];

    programs = ['program1', 'program2', 'program3'];

    selMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];

    programSelected = false;

    title = 'Πρωτογενές Αίτημα & Έγκριση Δαπάνης';

    constructor(private fb: FormBuilder) {
        this.createForm();
    }

    createForm() {
        this.newRequestForm = this.fb.group({
            name: ['', Validators.pattern('^\\w+\\s\\w+$') ],
            institute: '',
            program: '',
            position: '',
            requestText: '',
            supplier: '',
            supplierSelectionMethod: '',
            ammount: ['', Validators.pattern('^\\d*(\\.\\d{1,2})?$') ],
            director: 'kapoios'
        });
    }

    submitRequest(): boolean {
        this.request = new Request();
        this.request.stage1 = new Stage1();
        this.request.stage1.supplier = this.newRequestForm.get('supplier').value;
        this.request.stage1.supplierSelectionMethod = this.newRequestForm.get('supplierSelectionMethod').value;
        this.request.stage1.subject = this.newRequestForm.get('requestText').value;
        this.request.stage1.amountInEuros = this.newRequestForm.get('ammount').value;
        // this.request.stage1.attachment. = this.newRequestForm.get('').value;
        this.request.project = new Project();
        this.request.project.acronym = this.newRequestForm.get('program').value;

        this.createForm();
        return true;
    }

    // constructor() { }
    show(event: any) {
        if (event.target.value) {
            this.programSelected = true;
        }
    }

    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    ngOnInit() {}

}
