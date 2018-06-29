import { Component, ComponentFactoryResolver, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { StageDirective } from './stage.directive';
import { StageItem } from './stage-item';
import { StageInterfaceComponent } from './stage-interface.component';

@Component({
    selector: 'app-stage-loader',
    template: `<ng-template stage-host></ng-template>`
})
export class StagesLoaderComponent implements OnInit {
    @Input() stages: StageItem[] = [];
    currentIndex = -1;
    @ViewChild(StageDirective) stageHost: StageDirective;

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
        const stageItem = this.stages[this.currentIndex];

        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(stageItem.component);

        const viewContainerRef = this.stageHost.viewContainerRef;
        viewContainerRef.clear();

        const componentRef = viewContainerRef.createComponent(componentFactory);
        componentRef.instance['emitStage'].subscribe(emitted => this.emitStage.emit(emitted));
        componentRef.instance['emitFile'].subscribe(emitted => this.emitFile.emit(emitted));
        componentRef.instance['emitGoBack'].subscribe(emitted => this.emitGoBack.emit(emitted));
        componentRef.instance['newValues'].subscribe(emitted => this.newValues.emit(emitted));

        (<StageInterfaceComponent>componentRef.instance).data = stageItem.data;
    }
}
