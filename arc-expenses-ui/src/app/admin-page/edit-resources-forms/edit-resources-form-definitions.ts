import { FormArray, FormControl, FormGroup } from '@angular/forms';

export const delegateFormDefinition = {
    email: new FormControl(''),
    firstname: new FormControl(''),
    lastname: new FormControl(''),
    hidden: new FormControl('')
};

export const poiFormDefinition = {
    email: new FormControl(''),
    firstname: new FormControl(''),
    lastname: new FormControl(''),
    delegates: new FormArray([new FormGroup(delegateFormDefinition)])
};

export const organizationFormDefinition = {
    id: new FormControl(''),
    name: new FormControl(''),
    POI: new FormGroup(poiFormDefinition),
    director: new FormGroup(poiFormDefinition),
    dioikitikoSumvoulio: new FormGroup(poiFormDefinition),
    inspectionTeam: new FormArray([new FormGroup(poiFormDefinition)])
};

export const instituteFormDefinition = {
    id: new FormControl(''),
    name: new FormControl(''),
    organization: new FormControl('ARC'),
    director: new FormGroup(poiFormDefinition),
    accountingRegistration: new FormGroup(poiFormDefinition),
    accountingPayment: new FormGroup(poiFormDefinition),
    accountingDirector: new FormGroup(poiFormDefinition),
    diaugeia: new FormGroup(poiFormDefinition),
    suppliesOffice: new FormGroup(poiFormDefinition)
};

export const projectFormDefinition = {
    id: new FormControl(''),
    name: new FormControl(''),
    acronym: new FormControl(''),
    institute: new FormGroup(instituteFormDefinition),
    parentProject: new FormControl(''),
    scientificCoordinator: new FormGroup(poiFormDefinition),
    operator: new FormControl(''),
    startDate: new FormControl(''),
    endDate: new FormControl('')
};
