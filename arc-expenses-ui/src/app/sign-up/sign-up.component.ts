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
  showSpinner: boolean;

  signUpForm: FormGroup;

  firstnameLatin: string;
  lastnameLatin: string;
  userEmail: string;

  constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {
  }

  ngOnInit() {
      if (this.authService.getUserFirstName()) {
          this.router.navigate(['/home']);
      }
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
      this.signUpForm.get('nameLatin').disable();
      this.signUpForm.get('surnameLatin').disable();
      this.signUpForm.get('email').disable();
  }

  updateUser() {
      this.errorMessage = '';
      this.showSpinner = true;
      if (this.signUpForm.valid) {
          this.authService.updateUserInfo(this.signUpForm.get('name').value, this.signUpForm.get('surname').value ).subscribe(
              user => console.log(`updateUser responded: ${user}`),
              error => {
                  this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών';
                  this.showSpinner = false;
              },
              () => {
                  this.router.navigate(['/home']);
                  this.showSpinner = false;
              }
          );
      } else {
          this.errorMessage = 'Παρακαλώ συμπληρώστε τα στοιχεία σας.';
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

    getLastName() {
      return this.authService.getUserLastName();
    }

    getFirstName() {
      return this.authService.getUserFirstName();
    }

}
