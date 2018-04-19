import { Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import { StageFieldDescription } from '../domain/stageDescriptions';

@Component({
  selector: 'app-stage-form',
  template: `
<ng-template #descTemplate>{{description.description}}</ng-template>

<div class="uk-grid uk-form-horizontal">
    <!--<label class="uk-width-1-5 uk-form-label" *ngIf="description.label!=null" [ngClass]="{'required' : description.mandatory==true}">-->
    <label class="uk-width-1-5" *ngIf="description.label!=null" [ngClass]="{'required' : description.mandatory==true}">
        <!--<span *ngIf="description.mandatory==true && !valid"><i class="fa fa-star" style="color : red"></i></span>-->
        <!--<span *ngIf="description.recommended==true"><i class="fa fa-star" style="color : green"></i></span>-->
        {{description.label}}
        <span *ngIf="params==='tooltip'"><i class="fa fa-info-circle" [tooltip]="descTemplate" container="body"></i></span>
    </label>
    <input class="uk-form-controls uk-checkbox" type="{{description.type}}" [disabled]="isDisabled">
    <!--<div class="uk-width-expand@m uk-form-controls" [ngClass]="{'has-error': !valid}">-->
    <div class="uk-width-expand\@m" [ngClass]="{'has-error': !valid}">
        <ng-content></ng-content>
        <div *ngIf="params==='inline'">
            <i><small>{{description.description}}</small></i>
        </div>
    </div>
</div>

`
})
export class StageFormComponent implements OnChanges {

  constructor() { }

  @Input() public description: StageFieldDescription = null;

  @Input() public params: string = 'inline';

  @Input() public width: number = 9;

  @Input() public valid: boolean = true;

  @Input() public isDisabled: boolean = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.valid) {
        this.valid = <boolean>changes.valid.currentValue;
    }
  }

}
