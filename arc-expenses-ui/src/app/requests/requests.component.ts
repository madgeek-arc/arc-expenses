import { Component, OnInit } from '@angular/core';
import { BaseInfo, Project, Request, RequestSummary, Vocabulary } from '../domain/operation';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { Paging } from '../domain/extraClasses';
import {Router} from '@angular/router';
import { isNull, isNullOrUndefined } from 'util';
import { FormArray, FormBuilder, FormGroup } from '@angular/forms';
import { approvalStages, paymentStages, requestTypes, stageIds, stageTitles, statesList } from '../domain/stageDescriptions';
import { printRequestPage } from '../request-stage/print-request-function';
import { ManageResourcesService } from '../services/manage-resources.service';
import { ManageProjectService } from '../services/manage-project.service';
import { RequestInfo } from '../domain/requestInfoClasses';
import { forkJoin } from 'rxjs/observable/forkJoin';

@Component({
    selector: 'app-requests',
    templateUrl: './requests.component.html',
    styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

    title = 'Υπάρχοντα Αιτήματα';

    /* notifications */
    errorMessage: string;
    showSpinner: boolean;
    noRequests: string;

    /* data */
    stateNames = { all: 'Όλα', pending: 'Σε εξέλιξη', under_review: 'Σε εξέλιξη', rejected: 'Απορριφθέντα', accepted: 'Ολοκληρωθέντα'};
    stages: string[] = [];
    stagesMap = stageTitles;
    requestTypeIds = ['regular', 'contract', 'services_contract', 'trip'];
    reqTypes = requestTypes;
    projects: Vocabulary[] = [];
    institutes: Map<string, string> = new Map<string, string>();
    instituteIds: string[] = [];

    /* flags */
    isSimpleUser: boolean;
    allStatusSelected: boolean;
    allStagesSelected: boolean;
    allPhasesSelected: boolean;
    allTypesSelected: boolean;
    allProjectsSelected: boolean;
    allInstitutesSelected: boolean;

    /* search params and relevant vars */
    phaseId: number;
    searchTerm: string;
    statusesChoice: string[] = [];
    stagesChoice: string[] = [];
    typesChoice: string[] = [];
    projectsChoice: string[] = [];
    institutesChoice: string[] = [];
    order: string;
    orderField: string;
    itemsPerPage: number;
    currentPage: number;
    totalPages: number;

    /* forms */
    keywordField: FormGroup;
    filtersForm: FormGroup;

    /* search result vars */
    searchResults: Paging<RequestSummary>;
    listOfRequests: RequestSummary[] = [];

    constructor(private requestService: ManageRequestsService,
                private resourceService: ManageResourcesService,
                private projectService: ManageProjectService,
                private authService: AuthenticationService,
                private fb: FormBuilder) {}

    ngOnInit() {
        this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');

        /* TODO: remove when projects and institutes are added */
        this.initializeParams();
        // this.getProjectsAndInstitutes();
    }

    initializeParams() {
        this.stages = approvalStages.concat(paymentStages);
        this.initializeFiltersForm();
        this.keywordField = this.fb.group({ keyword: [''] });
        this.searchTerm = '';
        this.statusesChoice.push('all');
        this.stagesChoice.push('all');
        this.typesChoice.push('all');
        this.projectsChoice.push('all');
        this.institutesChoice.push('all');
        this.phaseId = 0;
        this.currentPage = 0;
        this.itemsPerPage = 10;
        this.order = 'DESC';
        this.orderField = 'creation_date';
        this.totalPages = 0;

        this.getListOfRequests();
    }

    initializeFiltersForm() {
        this.filtersForm = this.fb.group({
            phases: this.createFormArray({phase: [false]}, 2),
            statusChoices: this.createFormArray({status: [false]}, 3),
            stageChoices: this.createFormArray({stage: [false]}, this.stages.length),
            typeChoices: this.createFormArray({type: [false]}, this.requestTypeIds.length)
        });
        // projectChoices: this.createFormArray({project: [false]}, this.projects.length),
        // instituteChoices: this.createFormArray({institute: [false]}, this.instituteIds.length)
    }

    createFormArray(def: any, length: number) {
        const newArray = this.fb.array([]);
        for (let i = 0; i < length; i++) {
            newArray.push(this.fb.group(def));
        }
        return <FormArray>newArray;
    }

    /* get the requestSummaries list according to the current params */
    getListOfRequests() {
        this.noRequests = '';
        this.errorMessage = '';
        this.listOfRequests = [];
        this.showSpinner = true;
        const currentOffset = this.currentPage * this.itemsPerPage;
        this.requestService.searchAllRequestSummaries(this.searchTerm,
            this.statusesChoice,
            this.stagesChoice,
            currentOffset.toString(),
            this.itemsPerPage.toString(),
            this.order,
            this.orderField,
            this.authService.getUserProp('email')).subscribe(
            res => {
                if (res && !isNull(res)) {
                    this.searchResults = res;
                    if (this.searchResults && !isNull(this.searchResults.results) &&
                        this.searchResults.results.length > 0 &&
                        !this.searchResults.results.some(x => isNull(x))) {

                        this.listOfRequests = this.searchResults.results;
                        console.log(`searchAllRequests sent me ${this.listOfRequests.length} requests`);
                        console.log(`total requests are ${this.searchResults.total}`);
                        console.log(this.listOfRequests);
                        this.totalPages = Math.ceil(this.searchResults.total / this.itemsPerPage);
                    }
                }
            },
            error => {
                console.log(error);
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την φόρτωση των αιτημάτων';
                this.showSpinner = false;
                this.totalPages = 0;
            },
            () => {
                this.showSpinner = false;
                this.errorMessage = '';
                if (this.listOfRequests.length === 0) {
                    this.noRequests = 'Δεν βρέθηκαν σχετικά αιτήματα.';
                }
            }
        );
    }

    sortBy (category: string) {
        if (this.orderField && this.orderField === category) {
            this.toggleOrder();
        } else {
            this.order = 'ASC';
            this.orderField = category;
        }
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleOrder() {
        if (this.order === 'ASC') {
            this.order = 'DESC';
        } else {
            this.order = 'ASC';
        }
        this.currentPage = 0;
    }

    getOrderSign() {
        if (this.order === 'ASC') {
            return '&#9652;';
        } else {
            return '&#9662;';
        }
    }

    goToPreviousPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.getListOfRequests();
        }
    }

    goToNextPage() {
        if ( (this.currentPage + 1) < this.totalPages) {
            this.currentPage++;
            this.getListOfRequests();
        }
    }

    getItemsPerPage(event: any) {
        this.itemsPerPage = event.target.value;
        this.currentPage = 0;
        this.getListOfRequests();
    }

    choosePhase() {
        this.getPhaseId();
        this.stages = [];
        this.stagesChoice = [];
        if (this.phaseId === 0) {
            this.stages = approvalStages.concat(paymentStages);
        } else if (this.phaseId === 1) {
            this.stages = approvalStages;
        } else {
            this.stages = paymentStages;
        }
        this.setAllStageValues(false);
        this.initFormArray('stageChoices', { stage: [false] }, this.stages.length);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    initFormArray(arrayName: string, definition: any, length: number) {
        const formArray = <FormArray>this.filtersForm.controls[arrayName];
        formArray.controls = [];
        for (let i = 0; i < length; i++) {
            formArray.push(this.fb.group(definition));
        }
        console.log('formArray length is', formArray.length);
    }


    chooseStage() {
        if ( !this.isSimpleUser ) {
            this.getStageChoices();
            console.log('after getStageChoices list is', JSON.stringify(this.stagesChoice));
            this.currentPage = 0;
            this.getListOfRequests();
        }
    }

    chooseState() {
        this.getStatusChoices();
        console.log('after getStatusChoices list is', JSON.stringify(this.statusesChoice));
        this.currentPage = 0;
        this.getListOfRequests();
    }

    chooseType() {
        this.getTypeChoices();
        console.log('after getTypeChoices list is', JSON.stringify(this.typesChoice));
        this.currentPage = 0;
        this.getListOfRequests();
    }

    chooseProject() {
        this.getProjectChoices();
        console.log('after getProjectChoices list is', JSON.stringify(this.projectsChoice));
        this.currentPage = 0;
        this.getListOfRequests();
    }

    chooseInstitute() {
        this.getInstituteChoices();
        console.log('after getInstituteChoices list is', JSON.stringify(this.institutesChoice));
        this.currentPage = 0;
        this.getListOfRequests();
    }

    getSearchByKeywordResults() {
        this.searchTerm = this.keywordField.get('keyword').value;
        console.log('this.searchTerm is', this.searchTerm);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllPhases(event: any) {
        this.setAllPhaseValues(event.target.checked);
        this.setAllStageValues(false);
        this.stages = [];
        this.stages = approvalStages.concat(paymentStages);
        this.initFormArray('stageChoices', { stage: [false]}, this.stages.length);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllStages(event: any) {
        this.setAllStageValues(event.target.checked);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllStatuses(event: any) {
        this.setAllStatusValues(event.target.checked);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllTypes(event: any) {
        this.allTypesSelected = event.target.checked;
        this.setChoices(event.target.checked, 'typeChoices', 'type');
        this.typesChoice = [];
        this.typesChoice.push('all');
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllProjects(event: any) {
        this.allProjectsSelected = event.target.checked;
        this.setChoices(event.target.checked, 'projectChoices', 'project');
        this.projectsChoice = [];
        this.projectsChoice.push('all');
        this.currentPage = 0;
        this.getListOfRequests();
    }

    toggleSearchAllInstitutes(event: any) {
        this.allInstitutesSelected = event.target.checked;
        this.setChoices(event.target.checked, 'instituteChoices', 'institute');
        this.institutesChoice = [];
        this.institutesChoice.push('all');
        this.currentPage = 0;
        this.getListOfRequests();
    }

    setChoices(val: boolean, arrayName: string, controlName: string) {
        const formArray = <FormArray>this.filtersForm.controls[arrayName];
        formArray.controls.map( x => x.get(controlName).setValue(val) );
    }

    setAllPhaseValues(val: boolean) {
        this.allPhasesSelected = val;
        this.phaseId = 0;
        const phases = <FormArray>this.filtersForm.controls['phases'];
        phases.controls.map(x => x.get('phase').setValue(val));
    }

    setAllStageValues(val: boolean) {
        if ( !this.isSimpleUser ) {
            this.allStagesSelected = val;
            this.setChoices(val, 'stageChoices', 'stage');
            this.stagesChoice = [];
            if (this.phaseId === 0) {
                this.stagesChoice.push('all');
            } else if (this.phaseId === 1) {
                this.stagesChoice = approvalStages;
            } else {
                this.stagesChoice = paymentStages;
            }
        }
    }

    setAllStatusValues(val: boolean) {
        this.allStatusSelected = val;
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        statusChoices.controls.map(x => x.get('status').setValue(val));
        this.statusesChoice = [];
        this.statusesChoice.push('all');
    }

    getChoicesIndices(arrayName: string, controlName: string) {
        const formArray = <FormArray>this.filtersForm.controls[arrayName];
        const choicesIndices = [];
        formArray.controls.map( (x, i) => { if ( x.get(controlName).value ) { choicesIndices.push(i); } } );
        return choicesIndices;
    }

    getPhaseId() {
        this.allPhasesSelected = null;
        this.phaseId = 0;
        const phases = <FormArray>this.filtersForm.controls['phases'];
        if ( phases.at(0).get('phase').value ) {
            this.phaseId = 1;
        }
        if ( phases.at(1).get('phase').value ) {
            if (this.phaseId === 1) {
                this.phaseId = 0;
                this.allPhasesSelected = true;
            } else {
                this.phaseId = 2;
            }
        }
        console.log('phaseId is', this.phaseId);
    }

    getStageChoices() {
        if ( !this.isSimpleUser ) {
            this.allStagesSelected = null;
            this.stagesChoice = [];
            const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
            stageChoices.controls.map( (x, i) => { if ( x.get('stage').value ) { this.stagesChoice.push(this.stages[i]); } });
            if ((this.stagesChoice.length === 0) || (this.stagesChoice.length === this.stages.length)) {
                this.allStagesSelected = (this.stagesChoice.length === this.stages.length);
                this.stagesChoice = [];
                if (this.phaseId === 0) {
                    this.stagesChoice.push('all');
                } else if (this.phaseId === 1) {
                    this.stagesChoice = approvalStages;
                } else {
                    this.stagesChoice = paymentStages;
                }
            }
        }
    }

    getStatusChoices() {
        this.allStatusSelected = null;
        this.statusesChoice = [];
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        if ( statusChoices.at(0).get('status').value ) {
            this.statusesChoice.push('pending');
            this.statusesChoice.push('under_review');
        }
        if ( statusChoices.at(1).get('status').value ) {
            this.statusesChoice.push('rejected');
        }
        if ( statusChoices.at(2).get('status').value ) {
            this.statusesChoice.push('accepted');
        }
        if ((this.statusesChoice.length === 0) || (this.statusesChoice.length === 4) ) {
            this.allStatusSelected = (this.statusesChoice.length === 4);
            this.statusesChoice = [];
            this.statusesChoice.push('all');
            console.log(this.allStatusSelected);
        }
    }

    getTypeChoices() {
        this.allTypesSelected = null;
        this.typesChoice = [];
        const typeChoicesIndices = this.getChoicesIndices('typeChoices', 'type');
        if ((typeChoicesIndices.length === 0) || (typeChoicesIndices.length === this.projects.length)) {
            this.allTypesSelected = (typeChoicesIndices.length === this.requestTypeIds.length);
            this.typesChoice.push('all');
        } else {
            typeChoicesIndices.forEach( x => this.typesChoice.push(this.reqTypes[this.requestTypeIds[x]]) );
        }
    }

    getProjectChoices() {
        this.allProjectsSelected = null;
        this.projectsChoice = [];
        const projectChoicesIndices = this.getChoicesIndices('projectChoices', 'project');
        if ((projectChoicesIndices.length === 0) || (projectChoicesIndices.length === this.projects.length)) {
            this.allProjectsSelected = (projectChoicesIndices.length === this.projects.length);
            this.projectsChoice.push('all');
        } else {
            projectChoicesIndices.forEach( x => this.projectsChoice.push(this.projects[x].projectID) );
        }
    }

    getInstituteChoices() {
        this.allInstitutesSelected = null;
        this.institutesChoice = [];
        const instituteChoicesIndices = this.getChoicesIndices('instituteChoices', 'institute');
        if ((instituteChoicesIndices.length === 0) || (instituteChoicesIndices.length === this.instituteIds.length)) {
            this.allProjectsSelected = (instituteChoicesIndices.length === this.instituteIds.length);
            this.institutesChoice.push('all');
        } else {
            instituteChoicesIndices.forEach( x => this.institutesChoice.push(this.instituteIds[x]) );
        }
    }


    getProjectsAndInstitutes() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.projects = [];

        forkJoin(this.projectService.getAllProjectsNames(),
                 this.resourceService.getInstituteNames)
            .subscribe(
                res => {
                    this.projects = res[0];
                    this.institutes = res[1];
                },
                error => {
                    console.log(error);
                    this.showSpinner = false;
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
                },
                () => {
                    this.showSpinner = false;
                    this.errorMessage = '';
                    if ( (isNullOrUndefined(this.projects) || (this.projects.length === 0)) ||
                         isNullOrUndefined(this.institutes) ) {
                        this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
                    } else {
                        this.initializeParams();
                    }
                }
            );
    }


    getStatusAsString( status: string ) {
        if ( (status === 'pending') || (status === 'under_review') ) {
            return 'σε εξέλιξη';
        } else if (status === 'accepted') {
            return 'ολοκληρωθηκε';
        } else {
            return 'απορρίφθηκε';
        }
    }

    getTrStyle(req: RequestSummary) {
        if (this.getIfUserCanEdit(req.baseInfo.id, req.request.id, req.request.project, req.baseInfo.stage, req.request.type)) {
            return '#f7f7f7';
        } else {
            return '';
        }
    }

    getIfUserCanEdit(id: string, requestId: string, project: Project, stage: string, type: string) {
        const newRequestInfo = new RequestInfo(id, requestId, project, (type === 'trip'));
        return (( this.authService.getUserRole() === 'ROLE_ADMIN' ) ||
                ( this.isSimpleUser && ((stage === '1') || (stage === '7')) ) ||
                ( newRequestInfo[stage].stagePOIs.some(
                        x => ( (x.email === this.authService.getUserProp('email')) ||
                                        x.delegates.some(y => y.email === this.authService.getUserProp('email')) )
                    )
                ) );
    }

    printRequest(): void {
        printRequestPage();
    }

}
