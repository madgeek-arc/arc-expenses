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

@Component({
    selector: 'app-requests',
    templateUrl: './requests.component.html',
    styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

    errorMessage: string;
    showSpinner: boolean;
    noRequests: string;

    isSimpleUser: boolean;

    title: string;

    phaseId: number;
    searchTerm: string;
    statusList: string[] = [];
    stagesChoice: string[] = [];
    projectsChoice: string[] = [];
    currentPage: number;
    itemsPerPage: number;
    order: string;
    orderField: string;
    totalPages: number;

    stateNames = { all: 'Όλα', pending: 'Σε εξέλιξη', under_review: 'Σε εξέλιξη', rejected: 'Απορριφθέντα', accepted: 'Ολοκληρωθέντα'};
    stages: string[] = [];
    stagesMap = stageTitles;
    reqTypes = requestTypes;
    projects: Vocabulary[] = [];
    institutes: Map<string, string> = new Map<string, string>();
    instituteIds: string[] = [];
    allStatusSelected: boolean;
    allStagesSelected: boolean;
    allPhasesSelected: boolean;

    searchResults: Paging<RequestSummary>;

    listOfRequests: RequestSummary[] = [];

    keywordField: FormGroup;
    filtersForm: FormGroup;

    constructor(private requestService: ManageRequestsService,
                private resourceService: ManageResourcesService,
                private projectService: ManageProjectService,
                private authService: AuthenticationService,
                private fb: FormBuilder,
                private router: Router) {}

    ngOnInit() {
        this.initializeParams();
        // this.getProjects();
        this.title = 'Υπάρχοντα Αιτήματα';
        this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');
    }

    initializeParams() {
        this.stages = approvalStages.concat(paymentStages);
        this.initializeFiltersForm();
        this.keywordField = this.fb.group({ keyword: [''] });
        this.searchTerm = '';
        this.statusList.push('all');
        this.stagesChoice.push('all');
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
            stageChoices: this.createFormArray({stage:[false]}, this.stages.length)
        });
        // projects: this.createProjectsArray(),
        // institutes: this.createInstitutesArray()
    }

    createFormArray(def: any, length: number) {
        const newArray = this.fb.array([]);
        for (let i=0; i<length; i++) {
            newArray.push(this.fb.group(def));
        }
        return <FormArray>newArray;
    }

    createStatusArray() {
        const newArray = this.fb.array([]);
        newArray.push(this.fb.group({status: [false]}));
        newArray.push(this.fb.group({status: [false]}));
        newArray.push(this.fb.group({status: [false]}));
        return <FormArray>newArray;
    }

    createStagesArray() {
        const newArray = this.fb.array([]);
        for (let i = 0; i < this.stages.length; i++) {
            newArray.push(this.fb.group({stage: [false]}));
        }
        return <FormArray>newArray;
    }

    createProjectsArray() {
        const newArray = this.fb.array([]);
        for (let i = 0; i < this.projects.length; i++) {
            newArray.push(this.fb.group({project: [false]}));
        }
        return <FormArray>newArray;
    }

    /* the param 'resource' of search/all method is always 'request' */
    getListOfRequests() {
        this.noRequests = '';
        this.errorMessage = '';
        this.listOfRequests = [];
        this.showSpinner = true;
        const currentOffset = this.currentPage * this.itemsPerPage;
        this.requestService.searchAllRequestSummaries(this.searchTerm,
            this.statusList,
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
        this.initFormArray('stageChoices',{stage: [false]}, this.stages.length);
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

    getSearchResults() {
        this.searchTerm = this.keywordField.get('keyword').value;
        console.log('this.searchTerm is', this.searchTerm);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    getFilterSearchResults() {
        this.getStatusChoices();
        this.getStageChoices();
        /*this.keywordField.get('keyword').setValue('');
        this.searchTerm = '';*/
        this.currentPage = 0;
        this.getListOfRequests();
    }

    clearFilterControls() {
        //  TODO: add projects and institutes
        // projects: this.createProjectsArray(),
        // institutes: this.createInstitutesArray()
        this.setAllStatusValues(false);
        this.setAllStageValues(false);
    }

    clearAllControls() {
        //  TODO: add projects and institutes
        // projects: this.createProjectsArray(),
        // institutes: this.createInstitutesArray()
        this.setAllStatusValues(false);
        this.setAllStageValues(false);
        this.keywordField.get('keyword').setValue('');
        this.searchTerm = '';
        this.phaseId = 0;
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

    printRequest(): void {
        printRequestPage();
    }

    toggleSearchAllStatuses(event: any) {
        this.setAllStatusValues(event.target.checked);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    setAllStatusValues(val: boolean) {
        this.allStatusSelected = val;
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        statusChoices.controls.map(x => x.get('status').setValue(val));
        this.statusList = [];
        this.statusList.push('all');
    }

    getStatusChoices() {
        this.allStatusSelected = null;
        this.statusList = [];
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        if ( statusChoices.at(0).get('status').value ) {
            this.statusList.push('pending');
            this.statusList.push('under_review');
        }
        if ( statusChoices.at(1).get('status').value ) {
            this.statusList.push('rejected');
        }
        if ( statusChoices.at(2).get('status').value ) {
            this.statusList.push('accepted');
        }
        if ((this.statusList.length === 0) || (this.statusList.length === 4) ) {
            this.allStatusSelected = (this.statusList.length === 4);
            this.statusList = [];
            this.statusList.push('all');
            console.log(this.allStatusSelected);
        }
    }

    toggleSearchAllPhases(event: any) {
        this.setAllPhaseValues(event.target.checked);
        this.setAllStageValues(false);
        this.stages = [];
        this.stages = approvalStages.concat(paymentStages);
        this.initFormArray('stageChoices',{ stage: [false]}, this.stages.length);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    setAllPhaseValues(val: boolean) {
        this.allPhasesSelected = val;
        this.phaseId = 0;
        const phases = <FormArray>this.filtersForm.controls['phases'];
        phases.controls.map(x => x.get('phase').setValue(val));
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

    toggleSearchAllStages(event: any) {
        this.setAllStageValues(event.target.checked);
        this.currentPage = 0;
        this.getListOfRequests();
    }

    setAllStageValues(val: boolean) {
        if ( !this.isSimpleUser ) {
            this.allStagesSelected = val;
            const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
            stageChoices.controls.map(x => x.get('stage').setValue(val));
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

    getStageChoices() {
        if ( !this.isSimpleUser ) {
            this.allStagesSelected = null;
            this.stagesChoice = [];
            const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
            console.log('this.stages is', JSON.stringify(this.stages));
            console.log('stage array length is', stageChoices.length);
            console.log('stage array is', JSON.stringify(stageChoices.value));
            stageChoices.controls.map( (x, i) => { if ( x.get('stage').value ) { this.stagesChoice.push(this.stages[i]); } });
            if ((this.stagesChoice.length === 0) || (this.stagesChoice.length === this.stages.length)) {
                console.log('stagesChoice length is', this.stagesChoice.length);
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

    clearProjectChoices() {
        this.projectsChoice = [];
        const projectChoices = <FormArray>this.filtersForm.controls['projectChoices'];
        projectChoices.controls.map(x => x.get('project').setValue(false));
    }

    getProjectChoices() {
        this.projectsChoice = [];
        const projectChoices = <FormArray>this.filtersForm.controls['projectChoices'];
        for (let i = 0; i < projectChoices.length; i++) {
            if (projectChoices.get('projectChoices').value === true) {
                this.projectsChoice.push(this.projects[i].projectID);
            }
        }
    }


    getProjects() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.projects = [];
        this.projectService.getAllProjectsNames().subscribe (
            projects => {
                this.projects = projects;
                console.log(this.projects);
            },
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
            },
            () => {
                this.showSpinner = false;
                this.errorMessage = '';
                if ( isNullOrUndefined(this.projects) || (this.projects.length === 0)) {
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
                } else {
                    this.initializeParams();
                    // this.getInstitutes();
                }
            }
        );
    }

    getInstitutes() {
        this.showSpinner = true;
        this.errorMessage = '';
        this.projects = [];
        this.resourceService.getInstituteNames().subscribe (
            insts => {
                this.institutes = insts;
                this.instituteIds = Array.from(this.institutes.keys());
                console.log(this.institutes);
            },
            error => {
                console.log(error);
                this.showSpinner = false;
                this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
            },
            () => {
                this.showSpinner = false;
                this.errorMessage = '';
                if ( isNullOrUndefined(this.institutes) || (this.instituteIds.length === 0)) {
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την ανάκτηση των απαραίτητων πληροφοριών.';
                } else {
                    this.initializeParams();
                }
            }
        );
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
        console.log('after getStatusChoices list is', JSON.stringify(this.statusList));
        this.currentPage = 0;
        this.getListOfRequests();
    }

    getTrStyle(req: RequestSummary) {
        if (this.getIfUserCanEdit(req.request.id, req.request.project, req.baseInfo.stage)) {
            return '#f7f7f7';
        } else {
            return '';
        }
    }

    getIfUserCanEdit(requestId: string, project: Project, stage: string) {
        const newRequestInfo = new RequestInfo(requestId, project);
        return ((this.authService.getUserRole() === 'ROLE_ADMIN') ||
            (newRequestInfo[stage].stagePOIs.some(
                    x => ((x.email === this.authService.getUserProp('email')) ||
                        x.delegates.some(y => y.email === this.authService.getUserProp('email')))
                )
            ) );
    }

    navigateToRequestPage(baseInfo: BaseInfo) {
        if (baseInfo.id.includes('a')) {
            this.router.navigate(['/requests/request-stage', baseInfo.id]);
        } else {
            this.router.navigate(['/requests/request-stage-payment', baseInfo.id]);
        }
    }

}
