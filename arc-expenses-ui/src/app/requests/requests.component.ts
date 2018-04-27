import { Component, OnInit } from '@angular/core';
import {
    analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc, StageDescription
} from '../domain/stageDescriptions';
import {Request, Stage1} from '../domain/operation';
import {FormBuilder, FormGroup} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {forEach} from '@angular/router/src/utils/collection';

@Component({
    selector: 'app-requests',
    templateUrl: './requests.component.html',
    styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

  title = 'Υπάρχοντα Αιτήματα';

  symbols = ['', '&#9662;', '&#9652;'];
  categories = ['Α/Α', 'Πρόγραμμα', 'Ινστιτούτο/Μονάδα', 'Αιτών', 'Ημ/νια', 'Στάδιο', 'Ποσό', 'Κατάσταση'];
  catSymbols = [this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0], this.symbols[0],
      this.symbols[0], this.symbols[0]];

  currentPage = 0;

  totalPages: number = 20;

  itemsPerPage = 10;

  stage_num = 0;

  requestsForm: FormGroup;

  exampleStageDesc: StageDescription = {
    delegateField: 'delegateFieldName',
    stageFields: [analiftheiYpoxrewsiDesc, checkLegalityDesc, checkRegularityDesc]
  };

  stages = ['0', '1', '2', '3', '3a', '3b', '4', '5', '6', '7', '8', '9', '10'];
  states = ['Σε εξέλιξη', 'Απορρίφθηκε', 'Εγκρίθηκε']

  customStage1: Stage1 = {supplierSelectionMethod: 'kati', supplier: 'Plaisio', requestDate: '12/3/2018', amountInEuros: 234.56,
      attachment: null, subject: 'Θελω να αγορασω mousepad'};

  listOfRequests: Request [] = [
    {id: '1', project: null, requester: null, requesterPosition: 'θεση1', stage: 'stage2', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '2', project: null, requester: null, requesterPosition: 'θεση3', stage: 'stage3', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '3', project: null, requester: null, requesterPosition: 'θεση6', stage: 'stage9', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '4', project: null, requester: null, requesterPosition: 'θεση9', stage: 'stage4', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '5', project: null, requester: null, requesterPosition: 'θεση2', stage: 'stage3a', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '6', project: null, requester: null, requesterPosition: 'θεση4', stage: 'stage10', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '7', project: null, requester: null, requesterPosition: 'θεση7', stage: 'stage7', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '1', project: null, requester: null, requesterPosition: 'θεση1', stage: 'stage2', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '2', project: null, requester: null, requesterPosition: 'θεση3', stage: 'stage3', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    {id: '3', project: null, requester: null, requesterPosition: 'θεση6', stage: 'stage9', status: 'pending', stage1: this.customStage1, stage2: null, stage3: null,
      stage3a: null, stage3b: null, stage4: null, stage5: null, stage6: null, stage7: null, stage8: null, stage9: null, stage10: null},
    ];

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.createForm();
  }

  createForm() {
    this.requestsForm = this.fb.group({
      searchText: '',
      stage: '',
      status: ''
    });
  }

  sortBy(category: number) {
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
  }

  goToPreviousPage() {
  }

  goToNextPage() {
  }

  getItemsPerPage(items: number) {
    this.itemsPerPage = items;
  }

  ngOnInit(): void {
  }

}
