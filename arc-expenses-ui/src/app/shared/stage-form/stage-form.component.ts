import { Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import { StageFieldDescription } from '../../domain/stageDescriptions';

@Component({
  selector: 'app-stage-form',
  template: `
<div class="uk-grid">
    <label class="uk-width-2-5" *ngIf="description.label!=null">
        {{description.label}}
    </label>
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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.valid) {
        this.valid = <boolean>changes.valid.currentValue;
    }
  }

}
