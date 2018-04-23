import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
  selector: 'app-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.scss']
})
export class SignUpComponent implements OnInit {

  signUpForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.signUpForm = this.fb.group( {
        name: '',
        surname: '',
        nameLatin: 'Babis',
        surnameLatin: 'Sougias',
        email: 'babsougias@asd.fkkkkkkkk'
    });
  }

  ngOnInit() {
  }


}
