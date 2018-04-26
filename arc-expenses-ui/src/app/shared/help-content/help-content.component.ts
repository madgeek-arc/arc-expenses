/**
 * Created by stefania on 7/17/17.
 */
import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HelpContentService } from './help-content.service';
import { Content, PageContent } from '../../domain/page-content';

@Component({
    selector: 'help-content',
    template: `
        <ng-template [ngIf]="contents && contents.length>0">
            <ng-template ngFor let-content [ngForOf]="contents">
                <div [innerHTML]="content.content" class="uk-margin-medium-bottom uk-section-secondary"></div>
            </ng-template>
        </ng-template>
    `,
})

export class HelpContentComponent implements OnInit {

    @Input('position')
    position: string;

    contents: Content[];
    errorMessage: string = null;

    constructor(private _helpContentService: HelpContentService, private route: ActivatedRoute, private router: Router) {
    }

    ngOnInit() {

        this.errorMessage = null;

        /*this._helpContentService.getActivePageContent(this.router.url).subscribe(
            pageContent => this.shiftThroughContent(pageContent),
            error => this.handleError(<any>error));*/

       console.log('called help service');
    }

    shiftThroughContent(pageContent: PageContent) {
        this.contents = pageContent.content[this.position];
    }

    isPresent() {
        return (this.contents && this.contents.length > 0);
    }

    handleError(error) {
        this.errorMessage = 'System error retrieving page content (Server responded: ' + error + ')';
    }
}

@Component({
    selector: 'aside-help-content',
    template: `
        <ng-template [ngIf]="contents && contents.length>0">
            <ng-template ngFor let-content [ngForOf]="contents">
                <div [innerHTML]="content.content" class="uk-card uk-card-body uk-card-secondary sidemenu uk-margin-bottom"></div>
            </ng-template>
        </ng-template>
    `,
})
export class AsideHelpContentComponent extends HelpContentComponent {

}
