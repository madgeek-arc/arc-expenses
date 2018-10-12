import {Component, OnInit} from '@angular/core';
import {EditResourcesComponent} from './edit-resources.components';
import {instituteFormDefinition} from './edit-resources-form-definitions';
import {Delegate} from '../../domain/operation';

@Component({
    selector: 'app-edit-institute',
    templateUrl: './edit-institute.component.html'
})
export class EditInstituteComponent extends EditResourcesComponent implements OnInit {
    directorDelegates: Delegate[] = [];

    ngOnInit() {
        this.resourceFormDefinition = instituteFormDefinition;
        super.ngOnInit();
    }

}
