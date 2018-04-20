import { Component, OnInit } from '@angular/core';
import {
    analiftheiYpoxrewsiDesc, approvedDesc, checkLegalityDesc, checkRegularityDesc, StageDescription
} from '../domain/stageDescriptions';
import { Request } from '../domain/operation';

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

  title = 'Υπάρχοντα Αιτήματα';

  name: string = 'kati';

  exampleStageDesc: StageDescription = {
      delegateField: 'delegateFieldName',
      stageFields: [analiftheiYpoxrewsiDesc, approvedDesc, checkLegalityDesc, checkRegularityDesc]
  };

  listOfRequests: Request [] = [
      {id: '1', project: null, requester: null, requesterPosition: 'Μπάμπης', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '2', project: null, requester: null, requesterPosition: 'Μήτσος', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '3', project: null, requester: null, requesterPosition: 'Τάκης', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '4', project: null, requester: null, requesterPosition: 'Βλαδίμηρος', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '5', project: null, requester: null, requesterPosition: 'Άλκης', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '6', project: null, requester: null, requesterPosition: 'Αγησίλαος', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
      {id: '7', project: null, requester: null, requesterPosition: 'Μιλτιάδης', stage: 'stage2', stage1: null, stage2: null, stage3: null,
          stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
  ];

  constructor() { }

  ngOnInit() {
  }

}
