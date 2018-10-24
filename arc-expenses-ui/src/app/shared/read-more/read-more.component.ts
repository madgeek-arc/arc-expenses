/**
 * Created by stefania on 4/6/17.
 */
import { Component, Input, AfterContentInit, ElementRef } from '@angular/core';

@Component({
    selector: 'read-more',
    template: `<div [class.collapsed]="isCollapsed" [style.height]="isCollapsed ? maxHeight+'px' : 'auto'">
                    <ng-content></ng-content>
                </div>
                <a *ngIf="isCollapsable" (click)="isCollapsed =! isCollapsed">View {{isCollapsed? 'more':'less'}}...</a>`,
    styles: [`
        div.collapsed {
            overflow: hidden;
        }
    `]
})
export class ReadMoreComponent implements AfterContentInit {

    // the text that need to be put in the container
    // @Input() text: string;

    // maximum height of the container
    @Input('maxHeight') maxHeight: number = 100;

    // set these to false to get the height of the expended container
    public isCollapsed: boolean = false;
    public isCollapsable: boolean = false;

    constructor(private elementRef: ElementRef) {
    }

    ngAfterContentInit() {
        let currentHeight = this.elementRef.nativeElement.getElementsByTagName('div')[0].offsetHeight;

        // collapsable only if the contents make container exceed the max height
        if (currentHeight > this.maxHeight) {
            this.isCollapsed = true;
            this.isCollapsable = true;
        } else {

        }
    }
}
