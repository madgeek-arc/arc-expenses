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
  stages = approvalStages.concat(paymentStages);
  stagesMap = stageTitles;
  reqTypes = requestTypes;
  projects: Vocabulary[] = [];
  institutes: Map<string, string> = new Map<string, string>();
  instituteIds: string[] = [];

  searchResults: Paging<RequestSummary>;

  listOfRequests: RequestSummary[] = [];

  keywordField: FormGroup;

  filtersForm: FormGroup;
  readonly filtersFormDefinition = {
      statusChoices: [''],
      stageChoices: [''],
      projects: [''],
  };
  //  institutes: [''],

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
                              statusChoices: this.createStatusArray(),
                              stageChoices: this.createStagesArray()
                        });
      // projects: this.createProjectsArray(),
      // institutes: this.createInstitutesArray()
  }

  createStatusArray() {
      const newArray = this.fb.array([]);
      newArray.push(this.fb.group({status: ['']}));
      newArray.push(this.fb.group({status: ['']}));
      newArray.push(this.fb.group({status: ['']}));
      return <FormArray>newArray;
  }

  createStagesArray() {
      const newArray = this.fb.array([]);
      for (let i = 0; i < this.stages.length; i++) {
          newArray.push(this.fb.group({stage: ['']}));
      }
      return <FormArray>newArray;
  }

  rebuildStagesArray() {
      const stagesArray = <FormArray>this.filtersForm.controls['stageChoices'];
      stagesArray.controls = [];
      this.filtersForm.get('stageChoices').setValue(this.createStagesArray());
  }

  createProjectsArray() {
      const newArray = this.fb.array([]);
      for (let i = 0; i < this.projects.length; i++) {
          newArray.push(this.fb.group({project: ['']}));
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
                                                  this.stagesChoice[0],
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

  choosePhase(event: any) {
      this.phaseId = +event.target.value;
      this.stages = [];
      this.stagesChoice = [];
      if (this.phaseId === 0) {
          this.stages = approvalStages.concat(paymentStages);
          this.stagesChoice.push('all');
      } else if (this.phaseId === 1) {
          this.stages = approvalStages;
          this.stagesChoice = this.stages;
      } else {
          this.stages = paymentStages;
          this.stagesChoice = this.stages;
      }
      this.keywordField.get('keyword').setValue('');
      this.searchTerm = '';
      this.currentPage = 0;
      this.filtersForm = null;
      this.initializeFiltersForm();
      this.getListOfRequests();
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
      this.keywordField.get('keyword').setValue('');
      this.searchTerm = '';
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

    toggleSearchAllStatuses(val: boolean) {
       this.setAllStatusValues(val);
       this.getListOfRequests();
    }

    setAllStatusValues(val: boolean) {
        this.statusList = [];
        this.statusList.push('all');
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        statusChoices.controls.map(x => x.get('status').setValue(val));
    }

    getStatusChoices() {
        this.statusList = [];
        const statusChoices = <FormArray>this.filtersForm.controls['statusChoices'];
        if ( statusChoices.at(0).get('status').value === true ) {
            this.statusList.push('pending');
            this.statusList.push('under_review');
        }
        if ( statusChoices.at(1).get('status').value === true ) {
            this.statusList.push('rejected');
        }
        if ( statusChoices.at(2).get('status').value === true ) {
            this.statusList.push('accepted');
        }
    }

    toggleSearchAllStages(val: boolean) {
      this.setAllStageValues(val);
      this.getListOfRequests();
    }

    setAllStageValues(val: boolean) {
        this.stagesChoice = [];
        this.stagesChoice.push('all');
        const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
        stageChoices.controls.map(x => x.get('stage').setValue(val));
    }

    getStageChoices() {
        if ( !this.isSimpleUser ) {
            this.stagesChoice = [];
            const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
            for (let i = 0; i < stageChoices.length; i++) {
                if (stageChoices.at(i).get('stage').value === true) {
                    this.stagesChoice.push(this.stages[i]);
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

    updateParams() {
      console.log(this.filtersForm.value);
    }


    chooseStage(stage: string) {
        if ( !this.isSimpleUser ) {
            const stage_i = this.stages.findIndex(x => x === stage);
            const stageChoices = <FormArray>this.filtersForm.controls['stageChoices'];
            const chooseStage = (stageChoices.at(stage_i).get('stage').value === 'true');
            if (chooseStage) {
                if (this.stagesChoice.some(x => x === 'all')) {
                    this.stagesChoice = [];
                }
                this.stagesChoice.push(stage);
            } else {
                const i = this.stagesChoice.findIndex(x => x === stage);
                this.stagesChoice.splice(i, 1);
                if (this.stagesChoice.length === 0) {
                    this.stagesChoice.push('all');
                }
            }
            console.log(`this.stagesChoice is ${this.stagesChoice}`);
            this.keywordField.get('keyword').setValue('');
            this.searchTerm = '';
            this.currentPage = 0;
            this.getListOfRequests();
        }
    }

    chooseState(state: string, chooseStage: boolean) {
        if (chooseStage) {
            if (this.statusList.some(x => x === 'all')) {
                this.statusList = [];
            }
            this.statusList.push(state);
            if (state === 'pending') {
                this.statusList.push('under_review');
            }
        } else {
            let i = this.statusList.findIndex(x => x === state);
            this.stagesChoice.splice(i, 1);
            if (state === 'pending') {
                i = this.statusList.findIndex(x => x === 'under_review');
                this.stagesChoice.splice(i, 1);
            }
            if (this.stagesChoice.length === 0) {
                this.stagesChoice.push('all');
            }
        }
        console.log(`chosen status is ${this.statusList}`);
        this.keywordField.get('keyword').setValue('');
        this.searchTerm = '';
        this.currentPage = 0;
        this.getListOfRequests();
    }

    getIfUserCanEdit(requestId: string, project: Project, stage: string) {
      const newRequestInfo = new RequestInfo(requestId, project);
      return ((this.authService.getUserRole() === 'ROLE_ADMIN') ||
              (newRequestInfo[stage].stagePOIs.some(
                  x => ( x.email === this.authService.getUserProp('email') ||
                                  x.delegates.some(y => y.email === this.authService.getUserProp('email')))
                                 )
              ) );
    }

    navigateToRequestPage(baseInfo: BaseInfo) {
      if (baseInfo.id.includes('a')) {
          this.router.navigate(['request-stage', baseInfo.id]);
      } else {
          this.router.navigate(['request-stage-payment', baseInfo.id]);
      }
    }

}
