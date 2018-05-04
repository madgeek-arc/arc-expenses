import { Component, OnInit } from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.scss']
})
export class SignUpComponent implements OnInit {

  errorMessage: string;

  signUpForm: FormGroup;

  firstnameLatin: string;
  lastnameLatin: string;
  userEmail: string;

  constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {
  }

  ngOnInit() {
      this.createForm();
  }

  createForm () {
      console.log(this.firstnameLatin, this.lastnameLatin, this.userEmail);
      this.signUpForm = this.fb.group( {
          name: ['', Validators.required],
          surname: ['', Validators.required],
          nameLatin: '',
          surnameLatin: '',
          email:  ''
      });
  }

  updateUser() {
      if (this.signUpForm.valid) {
          this.authService.updateUserInfo(this.signUpForm.get('name').value, this.signUpForm.get('surname').value ).subscribe(
              user => console.log(`updateUser responded: ${user}`),
              error => this.errorMessage = 'Παρουσιάστηκε πρόβλημα με την αποθήκευση των αλλαγών',
              () => this.router.navigate(['/home'])
          );
      } else {
          this.errorMessage = 'Παρακαλώ συμπληρώστε τα στοιχεία σας';
      }
  }

  continueToHome() {
      this.router.navigate(['/home']);
  }

  getFirstNameInLatin() {
      return this.authService.getUserFirstNameInLatin();
  }

    getLastNameInLatin() {
        return this.authService.getUserLastNameInLatin();
    }

    getUserEmail() {
        return this.authService.getUserEmail();
    }

}
