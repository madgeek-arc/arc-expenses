import { Component, OnInit } from '@angular/core';
import { Request } from '../domain/operation';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { Paging } from '../domain/extraClasses';
import {Router} from '@angular/router';
import {isNull} from 'util';
import {FormBuilder, FormGroup} from '@angular/forms';
import { stagesMap } from '../domain/stageDescriptions';

@Component({
    selector: 'app-requests',
    templateUrl: './requests.component.html',
    styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

  errorMessage: string;
  showSpinner: boolean;
  noRequests: string;

  title = 'Υπάρχοντα Αιτήματα';

  searchTerm: string;
  currentPage: number;
  itemsPerPage: number;
  order: string;
  orderField: string;
  totalPages: number;

  stateNames = { all: 'Όλα', pending: 'Σε εξέλιξη', rejected: 'Έχουν απορριφθεί', accepted: 'Έχουν εγκριθεί'};
  states = ['all', 'accepted', 'pending', 'rejected'];
  stages = ['2', '3', '4', '5', '5a', '5b', '6', '7', '8', '9', '10', '11', '12'];
  stagesMap = stagesMap;

  searchResults: Paging<Request>;

  listOfRequests: Request [] = [];

  keywordField: FormGroup;

  constructor(private requestService: ManageRequestsService,
              private authService: AuthenticationService, private router: Router, private fb: FormBuilder) {}

  ngOnInit(){
      this.initializeParams();
  }

  initializeParams() {
      this.keywordField = this.fb.group({ keyword: [''] });
      this.searchTerm = 'all';
      this.currentPage = 0;
      this.itemsPerPage = 10;
      this.order = 'ASC';
      this.orderField = 'creation_date';
      this.totalPages = 0;

      this.getListOfRequests();
  }

  /* the param 'resource' of search/all method is always 'request' */
  getListOfRequests() {
    this.noRequests = '';
    this.errorMessage = '';
    this.listOfRequests = [];
    this.showSpinner = true;
    this.requestService.searchAllRequests(this.searchTerm,
                                          this.currentPage.toString(),
                                          this.itemsPerPage.toString(),
                                          this.order,
                                          this.orderField,
                                          this.authService.getUserEmail()).subscribe(
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
        },
        () => {
            // this.searchTerm = 'all';
            this.showSpinner = false;
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
      this.getListOfRequests();
  }

  toggleOrder() {
      if (this.order === 'ASC') {
          this.order = 'DESC';
      } else {
          this.order = 'ASC';
      }
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
    this.getListOfRequests();
  }

    chooseStage(event: any) {
      this.searchTerm = event.target.value;
      console.log(`this.searchTerm is ${this.searchTerm}`);
      this.keywordField.get('keyword').setValue('');
      this.getListOfRequests();
    }

    chooseState(event: any) {
      this.searchTerm = event.target.value;
      console.log(`this.searchTerm is ${this.searchTerm}`);
      this.keywordField.get('keyword').setValue('');
      this.getListOfRequests();
    }

    getSearchResults() {
      this.searchTerm = this.keywordField.get('keyword').value;
      console.log('this.searchTerm is', this.searchTerm);
      this.getListOfRequests();
    }

    getStatusAsString( status: string ) {
      if (status === 'pending') {
          return 'σε εξέλιξη';
      } else if (status === 'accepted') {
          return 'εγκρίθηκε';
      } else {
          return 'απορρίφθηκε';
      }
}

}
