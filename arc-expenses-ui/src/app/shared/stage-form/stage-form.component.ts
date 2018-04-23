import { Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import { StageFieldDescription } from '../../domain/stageDescriptions';

@Component({
  selector: 'app-stage-form',
  template: `
<div class="uk-grid">
    <label class="uk-width-2-5" *ngIf="description.label!=null">
        {{description.label}}
    </label>
    <div class="uk-width-expand\@m" [ngClass]="{'uk-text-danger': !valid}">
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
