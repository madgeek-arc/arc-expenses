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

  requestId: string;
  currentRequest: Request;
  canEdit: boolean = false;

  testDelegate: Delegate = {
    email: 'asdf@gmail.com',
    firstname: 'Firstname',
    lastname: 'Lastname',
    hidden: false
  };

    stage3aTest: Stage3a = {
        organizationDirector: this.testDelegate,
        date: '',
        approved: true,
        comment: '',
        attachment: null,
    };

  stage3Test: Stage3 = {
    operator: this.testDelegate,
    date: '27/04/2018',
    analiftheiYpoxrewsi: true,
    fundsAvailable: true,
    approved: false,
    comment: '',
    attachment: null,
  };

    stage2Test: Stage2 = {
        scientificCoordinator: this.testDelegate,
        date: '22/4/2018',
        approved: true,
        comment: 'comment comment comment',
        attachment: null
    };

    exampleStageDesc: StageDescription = {
      delegateField: 'delegateFieldName',
      stageFields: [analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc]
  };

  constructor(private route: ActivatedRoute,
              private requestService: ManageRequestsService,
              private authService: AuthenticationService) { }

  ngOnInit() {
    this.getCurrentRequest();
    /*this.getIfUserCanEditRequest();*/
  }

  getCurrentRequest() {
    this.requestId = this.route.snapshot.paramMap.get('id');

    /*call api to get request info or throw errorMessage*/
    this.requestService.getRequestById(this.requestId, this.authService.getUserEmail()).subscribe(
        res => {
                this.currentRequest = res;
            },
        error => {
            console.log(error);
            this.errorMessage = '';
        }
    );
/*
    this.currentRequest = new Request();
    this.currentRequest.id = '3';
    this.currentRequest.project = new Project();
    this.currentRequest.project.name = '5';
    this.currentRequest.project.institute = new Institute();
    this.currentRequest.project.institute.name = 'ILSP';
    this.currentRequest.requester = new Requester();
    this.currentRequest.requester.firstname = 'First';
    this.currentRequest.requester.lastname = 'Requester';
    this.currentRequest.requesterPosition = 'requester position';
    this.currentRequest.stage1 = new Stage1();
    this.currentRequest.stage1.requestDate = '25/4/2018';
    this.currentRequest.stage1.subject = 'request subject';
    this.currentRequest.stage1.supplier = 'supplier name';
    this.currentRequest.stage1.supplierSelectionMethod = 'selection method';
    this.currentRequest.stage1.amountInEuros = 232.23;
    this.currentRequest.stage1.attachment = new Attachment();
    this.currentRequest.stage1.attachment.filename = 'filename.txt';
    this.currentRequest.stage = '3a';
    this.currentRequest.status = 'pending';
    this.currentRequest.stage2 = this.stage2Test;
    this.currentRequest.stage3 = this.stage3Test;
    this.currentRequest.stage3a = this.stage3aTest;
    this.currentRequest.stage3b = new Stage3b();
    this.currentRequest.stage4 = new Stage4();
    this.currentRequest.stage5 = new Stage5();
    this.currentRequest.stage6 = new Stage6();
    this.currentRequest.stage7 = new Stage7();
    this.currentRequest.stage8 = new Stage8();
    this.currentRequest.stage9 = new Stage9();
    this.currentRequest.stage10 = new Stage10();*/
  }

  getSubmittedStage(newStage: any) {
      console.log(`got ${JSON.stringify(newStage, null, 1)}`);
      const currentStageName = 'stage' + this.currentRequest.stage;
      console.log(`submitting as ${currentStageName}`);
      this.currentRequest[currentStageName] = newStage;
      if (newStage['approved']) {
          if (this.currentRequest.stage === '10' ) {
              this.currentRequest.status = 'approved';
          } else {
              this.currentRequest.status = 'pending';
          }
      } else if (this.currentRequest.stage === '6') {
          this.currentRequest.status = 'pending';
      } else {
          this.currentRequest.status = 'declined';
      }

      if (this.currentRequest.status !== 'declined') {
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
      this.requestService.userCanEditRequest(this.authService.getUserEmail()).subscribe(
          res => this.canEdit = res,
          error => {
              console.log(error);
              this.canEdit = false;
          }
      );
  }

  submitRequest() {
      /*update this.currentRequest*/
      this.requestService.updateRequest(this.currentRequest, this.authService.getUserEmail()).subscribe(
          res => console.log(`add Request responded: ${res}`),
          error => console.log(error),
          () => this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν επιτυχώς'
      );
  }

    willShowStage (stageField: string) {
      const stageNumber = stageField.split('stage');
      if ( (stageNumber[1] === this.currentRequest.stage) ) {
          /*if ( this.currentRequest.status !== 'declined' ) {
              /!*return this.requestService.userCanEditRequest(this.authService.getUserEmail()).subscribe(
                  res => this.canEdit = res,
                  error => {
                      console.log(error);
                      this.canEdit = false;
                  }
              );*!/
              return true;
          } else {
              return false;
          }*/
          return true;
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
