import { Component, OnInit } from '@angular/core';
import {Request} from '../request';

@Component({
  selector: 'app-new-request',
  templateUrl: './new-request.component.html',
  styleUrls: ['./new-request.component.scss']
})
export class NewRequestComponent implements OnInit {

  institutes = ['ILSP', 'IMSI', 'ISI', 'SPU', 'PPA'];

  programs = ['program1', 'program2', 'program3'];

  selMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];

  myRequest =  new Request(10, 'des new-request.component.ts',
      'SPU',
      'director',
      'program2',
      'Μπάμπης',
      'Διαγωνισμός',
      1000.5,
      'I request more money because I am greedy');

  submitted = false;

  onSubmit() { this.submitted = true; }

  // TODO: Remove this when we're done
  get diagnostic() { return JSON.stringify(this.myRequest); }

  constructor() { }

  ngOnInit() {
  }

}
