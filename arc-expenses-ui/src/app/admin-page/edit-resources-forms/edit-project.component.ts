import { Component, Input, OnInit } from '@angular/core';
import { Delegate, Institute, Organization, POI, Project } from '../../domain/operation';
import { ManageProjectService } from '../../services/manage-project.service';
import { isNullOrUndefined } from 'util';
import { FormArray, FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ManageResourcesService } from '../../services/manage-resources.service';
import { projectFormDefinition } from './edit-resources-form-definitions';

@Component({
    selector: 'app-edit-project',
    template: ``
})
export class EditProjectComponent implements OnInit {

    @Input() resourceId: string;
    currentProject: Project;

    editProjectForm: FormGroup;

    readonly projectFormDefinition = projectFormDefinition;

    instituteIDs: string[] = [];
    poiIDs: string[] = [];
    delegates: string[] = [];

    constructor(private fb: FormBuilder,
                private projectsService: ManageProjectService,
                private resourcesService: ManageResourcesService) {}

    ngOnInit() {
        this.getPOIids();
        if ( !isNullOrUndefined(this.resourceId) ) {
            this.getProject();
        } else {
            this.getInstituteIds();
        }
    }

    createForm() {
        this.editProjectForm = this.fb.group(this.projectFormDefinition);
    }

    getInstituteIds() {
        this.resourcesService.getAllInstitutes().subscribe(
            insts => {
                this.instituteIDs = insts;
            },
            error => {
                console.log('from getAllInstitutes:');
                console.log(error);
            }
        );
    }

    getPOIids() {
        /*this.resourcesService.getAllPOIs().subscribe(
            pois => this.poiIDs = pois,
            error => console.log(error)
        );*/
    }

    getDelegateIds() {
        /*this.resourcesService.getAllDelegates().subscribe(
            dels => this.delegates = dels,
            error => console.log(error)
        );*/
    }

    getProject() {
        this.projectsService.getProjectById(this.resourceId).subscribe(
            proj => {
                console.log(`got project with id: ${proj['id']}`);
                this.currentProject = proj;
            },
            error => {
                console.log(error);
            },
            () => {
                this.createForm();
            }
        );
    }

}
