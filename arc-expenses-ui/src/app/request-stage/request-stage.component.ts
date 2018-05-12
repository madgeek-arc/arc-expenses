import { Component, OnInit } from '@angular/core';
import {analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc, StageDescription} from '../domain/stageDescriptions';
import {
    Request, Institute, Delegate, Stage1, Stage2, Stage3, Requester, Project, Attachment,
    Stage3a, Stage3b, Stage4, Stage5, Stage6, Stage7, Stage8, Stage9, Stage10
} from '../domain/operation';
import {ActivatedRoute} from '@angular/router';
import { ManageRequestsService } from '../services/manage-requests.service';
import {AuthenticationService} from '../services/authentication.service';
import {containerStart} from '@angular/core/src/render3/instructions';

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

  getSubmittedStage(newStage: any) {
      console.log(`got ${JSON.stringify(newStage, null, 1)}`);
      const currentStageName = 'stage' + this.currentRequest.stage;
      console.log(`submitting as ${currentStageName}`);
      this.currentRequest[currentStageName] = newStage;
      if (newStage['approved']) {
          if (this.currentRequest.stage === '10' ) {
              this.currentRequest.status = 'accepted';
          } else {
              this.currentRequest.status = 'pending';
          }
      } else if (this.currentRequest.stage === '6') {
          this.currentRequest.status = 'pending';
      } else {
          this.currentRequest.status = 'rejected';
      }

      if (this.currentRequest.status !== 'rejected') {
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
          }
      }

      this.submitRequest();
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
          if (this.currentRequest.stage !== '3a' && this.currentRequest.stage !== '3b') {
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
          }
      }
  }

}
