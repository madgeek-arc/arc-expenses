import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { StageFieldDescription } from '../../domain/stageDescriptions';
import { Delegate } from '../../domain/operation';

@Component({
  selector: 'app-stage-form',
  template: `
<div class="uk-grid uk-margin-small-top">
    <label class="uk-width-1-3" *ngIf="description.label!=null">
        {{description.label}}
    </label>
    <div class="uk-width-expand" [ngClass]="{'uk-text-danger': !valid}">
        <ng-content></ng-content>
    </div>
</div>

`
})
export class StageFormComponent implements OnChanges {

  constructor() { }

  @Input() public description: StageFieldDescription = null;

  @Input() public valid: boolean = true;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.valid) {
        this.valid = <boolean>changes.valid.currentValue;
    }
  }

}

@Component({
    selector: 'app-stage-form-upload-file',
    template: `
<div data-tooltip title="Επιλέξτε αρχείο" (drop)="getDroppedFile($event)" (dragover)="allowDrop($event)">
    <div class="uk-link uk-placeholder uk-text-center uk-margin-top uk-width-1-1" uk-form-custom>
        <i class="uk-icon-cloud-upload uk-icon-medium uk-text-muted uk-margin-small-right"></i>
        <input type="file" name="selectedFile" (change)="getInput($event)">
        <span class="uk-link">Επισυνάψτε το αρχείο σας ρίχνοντάς το εδώ ή πατώντας εδώ</span>
        <div><span class="uk-text-bold">Επιλεγμένο αρχείο: </span>
            {{uploadedFile ? uploadedFile.name : "επιλέξτε αρχείο"}}
        </div>
    </div>
</div>
`
})
export class StageFormUploadFileComponent implements OnInit {

  uploadedFile: File;

  @Output() emitFile: EventEmitter<File> = new EventEmitter<File>();

  constructor() {}

  ngOnInit() {}

  getDroppedFile(event: any) {
    event.preventDefault();
    console.log(event.dataTransfer.files[0]);
    /*run script to upload file*/
    this.uploadedFile = <File>event.dataTransfer.files[0];
    console.log('this.droppedFile is : ', this.uploadedFile);
    this.emitFile.emit(this.uploadedFile);
  }

  getInput(event: any) {
    this.uploadedFile = event.target.files[0];
    console.log('this.uploadedFile is : ', this.uploadedFile);
    this.emitFile.emit(this.uploadedFile);
  }

  allowDrop(event: any) {
    event.preventDefault();
  }

}
