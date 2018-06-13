import { Component, OnInit } from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';
import { Attachment } from '../domain/operation';
import { isNullOrUndefined } from 'util';

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

  uploadedFile: File;
  userAttachment = new Attachment();

  constructor(private fb: FormBuilder, private authService: AuthenticationService, private router: Router) {}

  ngOnInit() {
      /* load page only if the user is logged in */
      if (this.authService.getUserEmail()) {
          /* load page only if firstname or lastname doesn't exist */
          /*if (this.authService.getUserFirstName() && this.authService.getUserLastName()) {
              this.router.navigate(['/home']);
          }*/
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
      if (!this.authService.getUserFirstName() || !this.authService.getUserLastName()) {
          this.signUpForm.get('receiveEmails').setValue(true);
          this.signUpForm.get('immediateEmails').setValue(true);
      }
    }

    toggleImmediateEmailsDisable() {
      if (this.signUpForm.get('receiveEmails').value) {
          this.signUpForm.get('immediateEmails').enable();
      } else {
          this.signUpForm.get('immediateEmails').setValue(false);
          this.signUpForm.get('immediateEmails').disable();
      }
    }

    createAttachment(): Attachment {
        const tempAttachment: Attachment = new Attachment();
        if (this.uploadedFile) {
            tempAttachment.filename = this.uploadedFile.name;
            tempAttachment.mimetype = this.uploadedFile.type;
            tempAttachment.size = this.uploadedFile.size;
            tempAttachment.url = '';
        }

        return tempAttachment;
    }

    updateUser() {
      this.errorMessage = '';
      this.showSpinner = true;
      if (this.signUpForm.valid) {
          if (this.uploadedFile) {
              // SEND IT SOMEWHERE AND UPDATE USER
              this.userAttachment = this.createAttachment();
          }

          this.authService.updateUserInfo( this.signUpForm.get('name').value,
                                           this.signUpForm.get('surname').value,
                                           (this.signUpForm.get('receiveEmails').value).toString(),
                                           (this.signUpForm.get('immediateEmails').value).toString(),
                                           this.userAttachment ).subscribe(
              user => console.log(`updateUser responded: ${JSON.stringify(user)}`),
              error => {
                  this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών';
                  this.showSpinner = false;
              },
              () => {
                  this.errorMessage = '';
                  this.showSpinner = false;
                  /*if ( !isNullOrUndefined(sessionStorage.getItem('state.location')) &&
                      (sessionStorage.getItem('state.location') !== '/sign-up') ) {
                      const state = sessionStorage.getItem('state.location');
                      sessionStorage.removeItem('state.location');
                      console.log('in sign-up returning to', state);
                      this.router.navigate([state]);
                  } else {*/
                      this.router.navigate(['/home']);
                  /*}*/
              }
          );
      } else {
          this.errorMessage = 'Είναι απαραίτητο να συμπληρώσετε το όνομα και το επίθετό σας στα ελληνικά.';
          this.showSpinner = false;
      }
    }


    getUploadedFile(file: File) {
        this.uploadedFile = file;
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
