import { Component, OnInit } from '@angular/core';
import { stagesMap } from '../domain/stageDescriptions';
import { Request } from '../domain/operation';
import { ActivatedRoute } from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import { AuthenticationService } from '../services/authentication.service';
import { isNull } from 'util';

@Component({
  selector: 'app-request-stage',
  templateUrl: './request-stage.component.html',
  styleUrls: ['./request-stage.component.scss']
})
export class RequestStageComponent implements OnInit {
  errorMessage: string;
  successMessage: string;
  showSpinner: boolean;

  requestId: string;
  currentRequest: Request;
  canEdit: boolean = false;
  nextStage: string;

  stages = ['2', '3', '4', '5', '5a', '5b', '6', '7', '8', '9', '10', '11', '12'];
  stagesMap = stagesMap;

  constructor(private route: ActivatedRoute,
              private requestService: ManageRequestsService,
              private authService: AuthenticationService) { }

  ngOnInit() {
    this.getCurrentRequest();
  }

  getCurrentRequest() {
    this.showSpinner = true;
    this.requestId = this.route.snapshot.paramMap.get('id');

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

  getNextStage(nextStage: string) {
    this.nextStage = nextStage;
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

      /*if (this.currentRequest.status !== 'rejected') {
          if (this.currentRequest.stage !== '3' &&
              this.currentRequest.stage !== '3a' &&
              this.currentRequest.stage !== '3b' &&
              this.currentRequest.stage !== '10') {

              this.currentRequest.stage = (+this.currentRequest.stage + 1).toString();
          } else {
              if (this.currentRequest.stage === '10') {
                  this.currentRequest.stage = '10';
              } else if (this.currentRequest.stage === '3') {
                  if (this.currentRequest.stage1.amountInEuros < 10000) {
                      this.currentRequest.stage = '4';
                  } else if (this.currentRequest.stage1.amountInEuros >= 10000 && this.currentRequest.stage1.amountInEuros < 20000) {
                      this.currentRequest.stage = '3a';
                  } else {
                      this.currentRequest.stage = '3b';
                  }
              } else {
                  this.currentRequest.stage = '4';
              }
          }*/
      this.currentRequest.stage = this.nextStage;
      /*}*/

      this.submitRequest();
  }

  submitRequest() {
      this.showSpinner = true;
      this.errorMessage = '';
      this.successMessage = '';

      /*update this.currentRequest*/
      this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe(
          res => console.log(`add Request responded: ${res}`),
          error => {
              console.log(error);
              this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών.';
          },
          () => {
              this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν.';
              this.showSpinner = false;
          }
      );
  }

    willShowStage (stageField: string) {
      const stageNumber = stageField.split('stage');
      if ( (stageNumber[1] === this.currentRequest.stage) ) {
          return this.canEdit;
      } else {
          for ( const id of this.stages ) {
              if ( id === this.currentRequest.stage ) {
                  return false;
              }
              if ( id === stageField ) {
                  return ( !isNull(this.currentRequest[`stage${stageField}`].date) );
              }
          }
          /*if (this.currentRequest.stage !== '3a' && this.currentRequest.stage !== '3b') {
              if ( stageNumber[1] === '3a' || stageNumber[1] === '3b' ) {
                  return ( (+this.currentRequest.stage > 3) && this.currentRequest[stageField].date );
              } else {
                  return ( +stageNumber[1] <  +this.currentRequest.stage );
              }
          } else {
              if (this.currentRequest.stage === '3a') {
                  if (stageNumber[1] === '3b' ) {
                      return false;
                  } else {
                      return ( +stageNumber[1] <=  3 );
                  }
              } else {
                  if (stageNumber[1] === '3a' ) {
                      return false;
                  } else {
                      return ( +stageNumber[1] <=  3 );
                  }
              }
          }*/
      }
  }

}
