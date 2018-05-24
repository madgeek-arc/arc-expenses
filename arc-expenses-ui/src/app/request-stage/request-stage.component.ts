import { Component, OnInit } from '@angular/core';
import { Request } from '../domain/operation';
import { ActivatedRoute } from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';

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
    this.isSimpleUser = (this.authService.getUserRole() === 'user');
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
      let newStage: string;
      if ( stage !== '13' && this.currentRequest.status !== 'rejected' ) {
          if (stage === '4') {
              newStage = '5a';
          } else if (stage === '5a') {
            if (this.currentRequest.stage1.amountInEuros > 20000) {
                newStage = '5b';
            } else {
                newStage = '6';
            }
        } else if (stage === '5b') {
            newStage = '6';
        } else {
            newStage = (+stage + 1).toString();
        }
      }
      console.log('next stage is', newStage);
      return newStage;
  }

  getPreviousStage(stage: string) {
      let newStage: string;
      if ( stage !== '2' ) {
        if (stage === '5a') {
            newStage = '4';
        } else if (stage === '5b') {
              newStage = '5a';
        } else if (stage === '6') {
            if (this.currentRequest.stage1.amountInEuros > 20000) {
                newStage = '5b';
            } else {
                newStage = '5a';
            }
        } else {
            newStage = (+stage - 1).toString();
        }
      }
      console.log('previous stage is', newStage);
      return newStage;
  }

  getSubmittedStage(newStage: any) {
      console.log(`got ${JSON.stringify(newStage, null, 1)}`);
      const currentStageName = 'stage' + this.currentRequest.stage;
      console.log(`submitting as ${currentStageName}`);
      this.currentRequest[currentStageName] = newStage;
      if (newStage['approved']) {
          if (this.currentRequest.stage === '12' ) {
              this.currentRequest.status = 'accepted';
          } else {
              this.currentRequest.status = 'pending';
          }
      } else if (this.currentRequest.stage === '6' || this.currentRequest.stage === '11') {
          this.currentRequest.status = 'pending';
      } else {
          this.currentRequest.status = 'rejected';
      }
      if (this.goBackOneStage) {
          this.currentRequest.stage = this.getPreviousStage(this.currentRequest.stage);
      } else {
          this.currentRequest.stage = this.getNextStage(this.currentRequest.stage);
      }

      this.submitRequest();
  }

  submitRequest() {
      this.showSpinner = true;
      this.errorMessage = '';
      this.successMessage = '';

      /*update this.currentRequest*/
      this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe(
          res => console.log(`update Request responded: ${res.id}, status=${res.status}`),
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
              if (this.authService.getUserRole() === 'admin') {
                  return true;
              } else {
                  return this.canEdit;
                  /* RESTORE THIS TO RESTORE CHECK USER'S PERMISSION TO EDIT !!! */
                  /*return true;*/
              }
          } else {
              for (const id of this.stages) {
                  if (id === stageField) {
                      return ( this.currentRequest[`stage${stageField}`] && this.currentRequest[`stage${stageField}`].date );
                  }
              }
          }
      }
  }

  checkIfHasReturnedToPrevious(stage: string) {
      const nextStage = this.getNextStage(stage);
      const stageField = 'stage' + nextStage;
      return (this.currentRequest[stageField] && this.currentRequest[stageField].date !== '');
  }

}
