import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Attachment, Request, Stage5b } from '../../domain/operation';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { supplierSelectionMethods, supplierSelectionMethodsMap } from '../../domain/stageDescriptions';
import { isNullOrUndefined, isUndefined } from 'util';

@Component({
    selector: 'stage1-form',
    templateUrl: './stage1-form.component.html'
})
export class Stage1FormComponent implements OnInit {
    errorMessage: string;

    @Input() currentRequest: Request;

    /* output variable that sends the new Stage back to the parent component
     * in order to call the api and update the request */
    @Output() emitRequest: EventEmitter<any> = new EventEmitter<any>();
    @Output() emitFile: EventEmitter<File> = new EventEmitter<File>();


    updateStage1Form: FormGroup;
    stage1AttachmentName = '';
    uploadedFile: File;
    requestedAmount: string;
    readonly amountLimit = 20000;
    readonly lowAmountLimit = 2500;
    showWarning: boolean;

    selMethods = supplierSelectionMethodsMap;
    isSupplierRequired: boolean;

    constructor(private fb: FormBuilder) {}

    ngOnInit() {
        this.createForm();
    }

    createForm() {
        this.updateStage1Form = this.fb.group({
            requestText: [''],
            supplier: [''],
            supplierSelectionMethod: [''],
            amount: ['', [Validators.min(0), Validators.pattern('^\\d+(\\.\\d{1,2})?$')] ]
        });
        this.updateStage1Form.get('requestText').setValue(this.currentRequest.stage1.subject);
        this.updateStage1Form.get('requestText').updateValueAndValidity();
        if ( this.currentRequest.stage1.supplier ) {
            this.updateStage1Form.get('supplier').setValue(this.currentRequest.stage1.supplier);
            this.updateStage1Form.get('supplier').updateValueAndValidity();
        }
        if ( this.currentRequest.stage1.supplierSelectionMethod ) {
            this.updateStage1Form.get('supplierSelectionMethod').setValue(this.currentRequest.stage1.supplierSelectionMethod);
            this.updateStage1Form.get('supplierSelectionMethod').updateValueAndValidity();
        }
        if ( !isNullOrUndefined(this.currentRequest.stage1.amountInEuros) ) {
            this.updateStage1Form.get('amount').setValue( (this.currentRequest.stage1.amountInEuros).toString() );
            this.updateStage1Form.get('amount').updateValueAndValidity();
            this.updateStage1Form.get('amount').markAsTouched();
        }

        if (this.currentRequest.stage1.attachment) {
            this.stage1AttachmentName = this.currentRequest.stage1.attachment.filename;
        }
        this.isSupplierRequired = (this.currentRequest.type !== 'trip');
    }


    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    updateStage1() {
        console.log(this.updateStage1Form);
        this.errorMessage = '';
        if (this.updateStage1Form.valid ) {
            if ( (+this.updateStage1Form.get('amount').value > this.lowAmountLimit) &&
                (+this.updateStage1Form.get('amount').value <= this.amountLimit) &&
                ( this.updateStage1Form.get('supplierSelectionMethod').value === this.selMethods['direct'] ) ) {

                this.errorMessage = 'Για αιτήματα άνω των 2.500 € η επιλογή προμηθευτή γίνεται μέσω διαγωνισμού ή έρευνας αγοράς.';

            } else if ( (+this.updateStage1Form.get('amount').value > this.amountLimit) &&
                ( ((this.currentRequest.type !== 'trip') && (this.currentRequest.type !== 'contract')) &&
                    (this.updateStage1Form.get('supplierSelectionMethod').value !== this.selMethods['Διαγωνισμός']) ) ) {

                this.errorMessage = 'Για ποσά άνω των 20.000 € οι αναθέσεις πρέπει να γίνονται μέσω διαγωνισμού.';
            } else if ( (this.currentRequest.type !== 'trip') && (this.currentRequest.type !== 'contract') &&
                ( this.updateStage1Form.get('supplierSelectionMethod').value !== this.selMethods['competition'] ) &&
                !this.updateStage1Form.get('supplier').value ) {

                this.errorMessage = 'Είναι υποχρεωτικό να προσθέσετε πληροφορίες για τον προμηθευτή.';
            } else if ( (( this.updateStage1Form.get('supplierSelectionMethod').value !== this.selMethods['direct'] ) &&
                ( (this.currentRequest.type !== 'trip') && (this.currentRequest.type !== 'contract') )) &&
                (isNullOrUndefined(this.uploadedFile) && isNullOrUndefined(this.currentRequest.stage1.attachment) )) {

                this.errorMessage = 'Για αναθέσεις μέσω διαγωνισμού ή έρευνας αγοράς η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else if ( (+this.updateStage1Form.get('amount').value > this.lowAmountLimit) &&
                        isNullOrUndefined(this.currentRequest.stage1.attachment) &&
                        isNullOrUndefined(this.uploadedFile) ) {

                this.errorMessage = 'Για αιτήματα άνω των 2.500 € η επισύναψη εγγράφων είναι υποχρεωτική.';
            } else {
                this.currentRequest.stage1.requestDate = Date.now().toString();
                this.currentRequest.stage1.subject = this.updateStage1Form.get('requestText').value;
                this.currentRequest.stage1.supplier = this.updateStage1Form.get('supplier').value;
                this.currentRequest.stage1.supplierSelectionMethod = this.updateStage1Form.get('supplierSelectionMethod').value;
                this.currentRequest.stage1.amountInEuros = +this.updateStage1Form.get('amount').value;
                if ( !isNullOrUndefined(this.uploadedFile) ) {
                    this.currentRequest.stage1.attachment = new Attachment();
                    this.currentRequest.stage1.attachment.filename = this.uploadedFile.name;
                    this.currentRequest.stage1.attachment.mimetype = this.uploadedFile.type;
                    this.currentRequest.stage1.attachment.size = this.uploadedFile.size;
                    this.currentRequest.stage1.attachment.url = '';
                    this.emitFile.emit(this.uploadedFile);
                }
                this.currentRequest.stage = '2';
                this.currentRequest.status = 'pending';

                if ( (this.currentRequest.stage1.amountInEuros > this.amountLimit) && isUndefined(this.currentRequest.stage5b) ) {
                    this.currentRequest.stage5b = new Stage5b();
                }

                this.emitRequest.emit(this.currentRequest);

            }

        } else {
            this.errorMessage = 'Τα πεδία που σημειώνονται με (*) είναι υποχρεωτικά';
        }
    }

    setIsSupplierReq(val: boolean) {
        this.isSupplierRequired = val;
    }

    checkIfSupplierIsRequired() {
        if (this.updateStage1Form.get('supplierSelectionMethod').value) {
            this.setIsSupplierReq( (this.updateStage1Form.get('supplierSelectionMethod').value !== 'Διαγωνισμός' ) );
        }
    }

    showAmount() {

        if ( !isNullOrUndefined(this.updateStage1Form.get('amount').value.trim()) &&
            this.updateStage1Form.get('amount').value.trim().includes(',')) {

            const temp = this.updateStage1Form.get('amount').value.replace(',', '.');
            this.updateStage1Form.get('amount').setValue(temp);
        }

        this.updateStage1Form.get('amount').updateValueAndValidity();
        if ( !isNaN(this.updateStage1Form.get('amount').value.trim()) ) {
            this.requestedAmount = this.updateStage1Form.get('amount').value.trim();
        }

        if ( this.updateStage1Form.get('amount').value &&
            (+this.updateStage1Form.get('amount').value > this.lowAmountLimit) &&
            (+this.updateStage1Form.get('amount').value <= this.amountLimit) &&
            (this.currentRequest.type === 'regular') ) {

            this.showWarning = true;
        } else {
            this.showWarning = false;
        }
    }

}
