import {Component, OnInit} from '@angular/core';

@Component({
    selector: 'app-admin-page',
    templateUrl: './admin-page.component.html'
})
export class AdminPageComponent implements OnInit {
    title = 'Επεξεργασία/Προσθήκη εγγραφών στην βάση';
    selectedResourceType: string = '';

    constructor() {}

    ngOnInit() {}

    selectType(resourceType: string) {
        this.selectedResourceType = resourceType;
    }

    getProjects() {}

    getInstitutes() {}

    getOrganizations() {}

    addProject() {}

    updateProject() {}

    addInstitute() {}

    updateInstitute() {}

    addOrganization() {}

    updateOrganization() {}
}
