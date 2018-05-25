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

  constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {}

  ngOnInit() {
      /* load page only if the user is logged in */
      if (this.authService.getUserEmail()) {
          /* load page only if firstname or lastname doesn't exist */
          if (this.authService.getUserFirstName() && this.authService.getUserLastName()) {
              this.router.navigate(['/home']);
          }
          this.createForm();
      } else {
          this.router.navigate(['/home']);
      }
  }

  createForm () {
      console.log(this.firstnameLatin, this.lastnameLatin, this.userEmail);
      this.signUpForm = this.fb.group( {
          name: ['', Validators.required],
          surname: ['', Validators.required],
          nameLatin: '',
          surnameLatin: '',
          email: '',
          receiveEmails: [''],
          immediateEmails: ['']
      });
      this.addUserInfoToForm();
      /*this.signUpForm.get('nameLatin').disable();
      this.signUpForm.get('surnameLatin').disable();
      this.signUpForm.get('email').disable();*/
  }

    addUserInfoToForm () {
      this.signUpForm.get('name').setValue(this.authService.getUserFirstName());
      this.signUpForm.get('surname').setValue(this.authService.getUserLastName());
      this.signUpForm.get('nameLatin').setValue(this.authService.getUserFirstNameInLatin());
      this.signUpForm.get('surnameLatin').setValue(this.authService.getUserLastNameInLatin());
      this.signUpForm.get('email').setValue(this.authService.getUserEmail());
      this.signUpForm.get('receiveEmails').setValue(this.authService.getUserReceiveEmails());
      this.signUpForm.get('immediateEmails').setValue(this.authService.getUserImmediateEmails());

      this.signUpForm.get('nameLatin').disable();
      this.signUpForm.get('surnameLatin').disable();
      this.signUpForm.get('email').disable();
      this.toggleImmediateEmailsDisable();
    }

    toggleImmediateEmailsDisable() {
      if (this.signUpForm.get('receiveEmails').value) {
          this.signUpForm.get('immediateEmails').enable();
      } else {
          this.signUpForm.get('immediateEmails').setValue(false);
          this.signUpForm.get('immediateEmails').disable();
      }
    }

    updateUser() {
      this.errorMessage = '';
      this.showSpinner = true;
      if (this.signUpForm.valid) {
          this.authService.updateUserInfo(this.signUpForm.get('name').value,
                                          this.signUpForm.get('surname').value,
                                          this.signUpForm.get('receiveEmails').value,
                                          this.signUpForm.get('immediateEmails').value ).subscribe(
              user => console.log(`updateUser responded: ${user}`),
              error => {
                  this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών';
                  this.showSpinner = false;
              },
              () => {
                  this.router.navigate(['/home']);
              }
          );
      } else {
          this.errorMessage = 'Είναι απαραίτητο να συμπληρώσετε το όνομα και το επίθετό σας στα ελληνικά.';
          this.showSpinner = false;
      }
  }

  continueToHome() {
      this.errorMessage = '';
      if ( this.authService.getUserFirstName() && this.authService.getUserLastName() ) {
          this.router.navigate(['/home']);
      } else {
          this.errorMessage = 'Είναι απαραίτητο να αποθηκεύσετε το όνομα και το επίθετό σας στα ελληνικά.';
      }
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
