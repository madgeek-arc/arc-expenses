import { Component, OnInit } from '@angular/core';
import { Request } from '../domain/operation';
import { ActivatedRoute } from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { isNullOrUndefined} from 'util';

@Component({
  selector: 'app-request-stage',
  templateUrl: './request-stage.component.html',
  styleUrls: ['./request-stage.component.scss']
})
export class RequestStageComponent implements OnInit {
  errorMessage: string;
  successMessage: string;
  showSpinner: boolean;

  isSimpleUser: boolean;
  requestId: string;
  currentRequest: Request;
  canEdit: boolean = false;
  goBackOneStage: boolean;
  stages = ['2', '3', '4', '5', '5a', '5b', '6', '7', '8', '9', '10', '11', '12', '13'];
  stateNames = { pending: 'βρίσκεται σε εξέλιξη', rejected: 'έχει απορριφθεί', accepted: 'έχει ολοκληρωθεί'};
  reqTypes = { regular: 'Πρωτογενές Αίτημα', trip: 'Ταξίδι', contract: 'Σύμβαση' };

  constructor(private route: ActivatedRoute,
              private requestService: ManageRequestsService,
              private authService: AuthenticationService) { }

  ngOnInit() {
    this.getCurrentRequest();
    this.isSimpleUser = (this.authService.getUserRole() === 'ROLE_USER');
    console.log(`current user role is: ${this.authService.getUserRole()}`);
  }

  getCurrentRequest() {
    this.showSpinner = true;
    this.requestId = this.route.snapshot.paramMap.get('id');
    this.errorMessage = '';

    /*call api to get request info or throw errorMessage*/
    this.requestService.getRequestById(this.requestId, this.authService.getUserEmail()).subscribe(
        res => {
                this.currentRequest = res;
            },
        error => {
            console.log(error);
            this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
            this.showSpinner = false;
        },
        () => {
            this.getIfUserCanEditRequest();
        }
    );
  }

  getIfUserCanEditRequest() {
      this.errorMessage = '';
      this.requestService.isEditable(this.currentRequest, this.authService.getUserEmail()).subscribe(
          res => this.canEdit = res,
          error => {
              console.log(error);
              this.canEdit = false;
              this.showSpinner = false;
              this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την ανάκτηση του αιτήματος.';
          },
          () => {
              this.showSpinner = false;
              console.log('this.canEdit is ', this.canEdit);
          }
      );
  }

  getRequestToGoBack(event: any) {
      this.goBackOneStage = true;
  }

  getNextStage(stage: string) {
      if ( stage !== '13' && this.currentRequest.status !== 'rejected' ) {
          if (stage === '4') {
              return '5a';
          } else if (stage === '5a') {
            if (this.currentRequest.stage1.amountInEuros > 20000) {
                return '5b';
            } else {
                return '6';
            }
          } else if (stage === '5b') {
              return '6';
          } else {
              return (+stage + 1).toString();
        }
      }
      /*console.log('next stage is', newStage);*/
      return this.currentRequest.stage;
  }

  getPreviousStage(stage: string) {
      if ( stage !== '2' ) {
        if (stage === '5a') {
            return '4';
        } else if (stage === '5b') {
            return '5a';
        } else if (stage === '6') {
            if (this.currentRequest.stage1.amountInEuros > 20000) {
                return '5b';
            } else {
                return '5a';
            }
        } else {
            return (+stage - 1).toString();
        }
      }
      /*console.log('previous stage is', newStage);*/
      return this.currentRequest.stage;
  }

  getSubmittedStage(newStage: any) {
      console.log(`got ${JSON.stringify(newStage, null, 1)}`);
      const currentStageName = 'stage' + this.currentRequest.stage;
      console.log(`submitting as ${currentStageName}`);
      this.currentRequest[currentStageName] = newStage;
      if (newStage['approved']) {
          if (this.currentRequest.stage === '13' ) {
              this.currentRequest.status = 'accepted';
          } else {
              this.currentRequest.status = 'pending';
          }
      } else if (this.currentRequest.stage === '6' || this.currentRequest.stage === '11' || (this.goBackOneStage === true) ) {
          this.currentRequest.status = 'pending';
      } else {
          this.currentRequest.status = 'rejected';
      }
      if (this.goBackOneStage === true) {
          this.currentRequest.stage = this.getPreviousStage(this.currentRequest.stage);
      } else {
          this.currentRequest.stage = this.getNextStage(this.currentRequest.stage);
      }
      this.goBackOneStage = false;
      console.log('submitted status:', this.currentRequest.status);
      this.submitRequest();
  }

  submitRequest() {
      this.showSpinner = true;
      this.errorMessage = '';
      this.successMessage = '';

      /*update this.currentRequest*/
      this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe(
          res => console.log(`update Request responded: ${res.id}, status=${res.status}, stage=${res.stage}`),
          error => {
              console.log(error);
              this.showSpinner = false;
              this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
          },
          () => {
              this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν.';
              this.showSpinner = false;
          }
      );
  }

  willShowStage (stageField: string) {
      if (!this.isSimpleUser) {
          if ((stageField === this.currentRequest.stage)) {
              if ( this.authService.getUserRole() === 'ROLE_ADMIN' ) {
                  return true;
              } else {
                  return this.canEdit;
              }
          } else {
              /*console.log('BOOM!');*/
              return (!isNullOrUndefined(this.currentRequest[`stage${stageField}`]) &&
                      !isNullOrUndefined(this.currentRequest[`stage${stageField}`].date) );
          }
      } else {
          return false;
      }
  }

  checkIfHasReturnedToPrevious(stage: string) {
      if ( (this.currentRequest.stage === stage) ) {
          /*if (stage === '13' || stage === '2' ||
              ( (this.currentRequest.status === 'rejected') ||
                  (this.currentRequest.status === 'accepted') ) ) {

              return 0;
          } else {
              const nextStage = this.getNextStage(stage);
              const stageField = `stage${nextStage}`;
              console.log(`nextStage is ${stageField} and this is its date: ${this.currentRequest[stageField].date}`);
              if (!(isNullOrUndefined(this.currentRequest[stageField])) &&
                  !(isNullOrUndefined(this.currentRequest[stageField].date)) ) {
                  console.log('sending 1');
                  return 1;
              } else {
                  return 0;
              }
          }*/
          if ( ( (this.currentRequest.status === 'rejected') ||
                  (this.currentRequest.status === 'accepted') ) ) {
              return 0;
          } else {
              return 1;
          }
      } else {
          for ( const st of this.stages ) {
              if ( st === stage ) {
                  return 0;
              }
              if ( st === this.currentRequest.stage ) {
                  return 2;
              }
          }
      }
  }

}
