import { Component, OnInit } from '@angular/core';
import { Request } from '../domain/operation';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { Paging } from '../domain/extraClasses';
import {Router} from '@angular/router';
import {isNull} from 'util';

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

  symbols = ['', '&#9662;', '&#9652;'];
  categories = ['Α/Α', 'Πρόγραμμα', 'Ινστιτούτο/Μονάδα', 'Αιτών', 'Ημ/νια', 'Στάδιο', 'Ποσό', 'Κατάσταση'];
  catSymbols = [this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0],
      this.symbols[0], this.symbols[0]];

  searchTerm: string;
  currentPage: number;
  itemsPerPage: number;
  order: string;
  orderField: string;

  states = ['Σε εξέλιξη', 'Απορρίφθηκε', 'Εγκρίθηκε'];
  stages = [ { 1: 'stage1' }, { 2: 'stage2' }, { 3: 'stage3' }, { 4: 'stage3a' }, { 5: 'stage3b' }, { 6: 'stage4' }, { 7: 'stage5' },
             { 8: 'stage6' }, { 9: 'stage7' }, { 10: 'stage8' }, { 11: 'stage9' }, { 12: 'stage10' }];
  stageTitles = ['1', '2', '3', '3a', '3b', '4', '5', '6', '7', '8', '9', '10'];

  searchResults: Paging<Request>;

  listOfRequests: Request [] = [];

  constructor(private requestService: ManageRequestsService,
              private authService: AuthenticationService, private router: Router) {}

  ngOnInit(){
      this.initializeParams();
  }

  initializeParams() {
      this.searchTerm = '';
      this.currentPage = 0;
      this.itemsPerPage = 10;
      this.order = '';
      this.orderField = '';

      this.getListOfRequests();
  }

  /* the param 'resource' of search/all method is always 'request' */
  getListOfRequests() {
    this.noRequests = '';
    this.errorMessage = '';
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
                    console.log(this.listOfRequests);
                }
            }
        },
        error => {
            console.log(error);
            this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την φόρτωση των αιτημάτων';
            this.showSpinner = false;
        },
        () => {
            this.searchTerm = '';
            this.showSpinner = false;
            if (this.listOfRequests.length === 0) {
                this.noRequests = 'Δεν βρέθηκαν σχετικά αιτήματα.';
            }
        }
    );
  }

  /*sortBy(category: number) {
    if (this.catSymbols[category] === this.symbols[0]) {
      // console.log(this.catSymbols); // DEBUG
      // this.catSymbols.forEach(x => x = this.symbols[0]);
      for (let i = 0; i < this.catSymbols.length; i++) {
        this.catSymbols[i] = this.symbols[0];
      }
        this.catSymbols[category] = this.symbols[1];
      // TODO: call sort asc

    } else if (this.catSymbols[category] === this.symbols[1]) {
      this.catSymbols[category] = this.symbols[2];
      // TODO: call sort desc

    } else if (this.catSymbols[category] === this.symbols[2]) {
      this.catSymbols[category] = this.symbols[1];
      // TODO: call sort asc
    }
  }*/

  sortBy (category: string) {
      if (this.orderField && this.orderField === category) {
          this.toggleOrder();
      } else {
          this.order = 'asc';
          this.orderField = category;
      }
      this.getListOfRequests();
  }

  toggleOrder() {
      if (this.order === 'asc') {
          this.order = 'desc';
      } else {
          this.order = 'asc';
      }
  }

  getOrderSign() {
      if (this.order === 'asc') {
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
      if (this.currentPage + 1 < this.searchResults.total) {
          this.currentPage++;
          this.getListOfRequests();
      }
  }

  getItemsPerPage(event: any) {
    this.itemsPerPage = event.target.value;
    this.getListOfRequests();
  }

    getSearchTerm (event: any) {
      this.searchTerm = event.target.value;
    }

    chooseState(event: any) {
      const val = event.target.value;
      if (val === 0) {
          this.searchTerm = 'pending';
      } else if ( val === 1 ) {
          this.searchTerm = 'approved';
      } else {
          this.searchTerm = 'declined';
      }
      this.getListOfRequests();
    }

    getSearchResults() {
      if (this.searchTerm) {
          console.log('this.searchTerm is', this.searchTerm);
          this.getListOfRequests();
      }
    }

    getStatusAsString( status: string ) {
      if (status === 'pending') {
          return 'σε εξέλιξη';
      } else if (status === 'approved') {
          return 'εγκρίθηκε';
      } else {
          return 'απορρίφθηκε';
      }
}

}
