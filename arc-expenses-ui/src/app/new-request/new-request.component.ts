import { Component, OnInit } from '@angular/core';
import { Request } from '../request';
import { FormBuilder, FormGroup, Validators} from '@angular/forms';

@Component({
  selector: 'app-new-request',
  templateUrl: './new-request.component.html',
  styleUrls: ['./new-request.component.scss']
})
export class NewRequestComponent implements OnInit {

  newRequestForm = FormGroup;
  // newRequestForm = new FormGroup ({
  //     name: new FormControl(),
  //     institute: new FormControl(),
  //     program: new FormControl(),
  //     position: new FormControl(),
  //     requestText: new FormControl(),
  //     supplier: new FormControl(),
  //     ssm: new FormControl(),
  //     ammount: new FormControl()
  // });

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

  constructor(private fb: FormBuilder) {
      this.createForm();
  }

  createForm() {
      this.newRequestForm = this.fb.group({
          name: ['', Validators.required ],
          institute: '',
          program: '',
          position: '',
          requestText: '',
          supplier: '',
          supplierSelectionMethod: '',
          ammount: ''
      });
  }


  // submitted = false;

  // onSubmit() { this.submitted = true; }

  constructor() { }

  ngOnInit() {
  }

}
