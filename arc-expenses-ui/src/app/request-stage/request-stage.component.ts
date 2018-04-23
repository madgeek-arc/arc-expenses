import { Component, OnInit } from '@angular/core';
import {analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc, StageDescription} from '../domain/stageDescriptions';
import {Request, Delegate, Stage2, Stage3} from '../domain/operation';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-request-stage',
  templateUrl: './request-stage.component.html',
  styleUrls: ['./request-stage.component.scss']
})
export class RequestStageComponent implements OnInit {

  title = 'Αίτημα X';
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
    const requestId = this.route.snapshot.paramMap.get('id');
    /*call api to get request info*/

    this.currentRequest = new Request();
    this.currentRequest.stage = '3';
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
  }

}
