import {Component, Input, OnInit} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {isNullOrUndefined} from 'util';
import {ManageProjectService} from '../../services/manage-project.service';
import {ManageResourcesService} from '../../services/manage-resources.service';

@Component({
    selector: 'app-edit-resources',
    template: ``
})
export class EditResourcesComponent implements OnInit {
    @Input() currentResource: any;

    resourceForm: FormGroup;
    resourceFormDefinition = null;
    currentResourceType: string;


    constructor(public fb: FormBuilder,
                private projectService: ManageProjectService,
                private resourcesService: ManageResourcesService) {}

    ngOnInit() {
        this.createForm();
    }

    createForm() {
        if ( !isNullOrUndefined(this.resourceFormDefinition) ) {
            this.resourceForm = this.fb.group(this.resourceFormDefinition);
        }
    }

    addResource() {}

    updateResource() {}

    deleteResource() {}

}
