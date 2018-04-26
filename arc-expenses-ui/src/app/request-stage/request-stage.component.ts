import { Component, OnInit } from '@angular/core';
import {analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc, StageDescription} from '../domain/stageDescriptions';
import {Request, Institute, Delegate, Stage1, Stage2, Stage3, Requester, Project, Attachment} from '../domain/operation';
import {ActivatedRoute} from '@angular/router';

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

  testDelegate: Delegate = {
    email: 'asdf@gmail.com',
    firstname: 'Firstname',
    lastname: 'Lastname',
    hidden: false
  };

  stage3Test: Stage3 = {
    operator: this.testDelegate,
    date: '22/4/2018',
    analiftheiYpoxrewsi: false,
    fundsAvailable: true,
    approved: true,
    comment: '',
    attachment: null,
  };

    stage2Test: Stage2 = {
        scientificCoordinator: this.testDelegate,
        date: '22/4/2018',
        approved: true,
        comment: '',
        attachment: null
    };

    exampleStageDesc: StageDescription = {
      delegateField: 'delegateFieldName',
      stageFields: [analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc]
  };

  constructor(private route: ActivatedRoute) { }

  ngOnInit() {
    this.getCurrentRequest();
  }

  getCurrentRequest() {
    this.requestId = this.route.snapshot.paramMap.get('id');
    /*call api to get request info or throw errorMessage*/

    this.currentRequest = new Request();
    this.currentRequest.id = 'request1';
    this.currentRequest.project = new Project();
    this.currentRequest.project.name = 'project 2';
    this.currentRequest.project.institute = new Institute();
    this.currentRequest.project.institute.name = 'Institute name';
    this.currentRequest.requester = new Requester();
    this.currentRequest.requester.firstname = 'Some';
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
    this.currentRequest.stage = '3';
    this.currentRequest.stage2 = this.stage2Test;
    this.currentRequest.stage3 = this.stage3Test;
  }

  getSubmittedStage(newStage: any) {
      console.log(`got ${JSON.stringify(newStage, null, 1)}`);
      const currentStageName = 'stage' + this.currentRequest.stage;
      console.log(`submitting as ${currentStageName}`);
      this.currentRequest[currentStageName] = newStage;
      if (this.currentRequest.stage !== '3' && this.currentRequest.stage !== '3a') {
          this.currentRequest.stage = (+this.currentRequest.stage + 1).toString();
      } else {
          if (this.currentRequest.stage === '3') {
              this.currentRequest.stage = '3a';
          } else {
              this.currentRequest.stage = '3b';
          }
      }

      this.submitRequest();
  }

  submitRequest() {
      /*submit this.currentRequest*/
      this.successMessage = 'Οι αλλαγές αποθηκεύτηκαν επιτυχώς';
  }

}
