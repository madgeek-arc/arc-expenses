import { Component, ComponentFactoryResolver, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { AnchorInterfaceComponent } from '../../shared/dynamic-loader-anchor-components/anchor-interface.component';
import {AnchorItem} from '../../shared/dynamic-loader-anchor-components/anchor-item';
import {AnchorDirective} from '../../shared/dynamic-loader-anchor-components/anchor.directive';

@Component({
    selector: 'app-stage-loader',
    template: `<ng-template anchor-host></ng-template>`
})
export class StagesLoaderComponent implements OnInit {
    @Input() stages: AnchorItem[] = [];
    currentIndex = -1;
    @ViewChild(AnchorDirective) stageHost: AnchorDirective;

    @Output() emitStage: EventEmitter<any> = new EventEmitter<any>();
    @Output() emitFile: EventEmitter<File> = new EventEmitter<File>();
    @Output() emitGoBack: EventEmitter<any> = new EventEmitter<any>();
    @Output() newValues: EventEmitter<string[]> = new EventEmitter<string[]>();

    constructor(private componentFactoryResolver: ComponentFactoryResolver) { }

    ngOnInit() {
        for (const stage of this.stages) {
            this.loadComponent();
        }
    }

    loadComponent() {
        this.currentIndex = (this.currentIndex + 1) % this.stages.length;
        const anchorItem = this.stages[this.currentIndex];

        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(anchorItem.component);

        const viewContainerRef = this.stageHost.viewContainerRef;
        viewContainerRef.clear();

        const componentRef = viewContainerRef.createComponent(componentFactory);
        componentRef.instance['emitStage'].subscribe(emitted => this.emitStage.emit(emitted));
        componentRef.instance['emitFile'].subscribe(emitted => this.emitFile.emit(emitted));
        componentRef.instance['emitGoBack'].subscribe(emitted => this.emitGoBack.emit(emitted));
        componentRef.instance['newValues'].subscribe(emitted => this.newValues.emit(emitted));

        (<AnchorInterfaceComponent>componentRef.instance).data = anchorItem.data;
    }
}
