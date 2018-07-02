import { Component, OnInit } from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AuthenticationService} from '../services/authentication.service';
import {Router} from '@angular/router';
import { Attachment } from '../domain/operation';
import { isNullOrUndefined } from 'util';
import { HttpEventType, HttpResponse } from '@angular/common/http';
import { ManageUserService } from '../services/manage-user.service';

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

  uploadedFile: File;
  userAttachment: Attachment;
  signatureFilename = '';

  constructor(private fb: FormBuilder,
              private authService: AuthenticationService,
              private userService: ManageUserService,
              private router: Router) {}

  ngOnInit() {
      /* load page only if the user is logged in */
      if (this.authService.getUserProp('email')) {

          this.createForm();
      } else {

          this.router.navigate(['/home']);
      }
  }

  createForm () {
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
  }

    addUserInfoToForm () {
      this.signUpForm.get('name').setValue(this.authService.getUserProp('firstname'));
      this.signUpForm.get('surname').setValue(this.authService.getUserProp('lastname'));
      this.signUpForm.get('nameLatin').setValue(this.authService.getUserProp('firstnameLatin'));
      this.signUpForm.get('surnameLatin').setValue(this.authService.getUserProp('lastnameLatin'));
      this.signUpForm.get('email').setValue(this.authService.getUserProp('email'));
      this.signUpForm.get('receiveEmails').setValue(this.authService.getUserProp('receiveEmails'));
      this.signUpForm.get('immediateEmails').setValue(this.authService.getUserProp('immediateEmails'));

      this.signUpForm.get('nameLatin').disable();
      this.signUpForm.get('surnameLatin').disable();
      this.signUpForm.get('email').disable();
      this.toggleImmediateEmailsDisable();
      if (!this.authService.getUserProp('firstname') || !this.authService.getUserProp('lastname')) {
          this.signUpForm.get('receiveEmails').setValue(true);
          this.signUpForm.get('immediateEmails').setValue(true);
          this.signUpForm.get('immediateEmails').enable();
      }
      if ( !isNullOrUndefined(this.authService.getSignatureAttachment()) ) {
          this.userAttachment = this.authService.getSignatureAttachment();
          this.signatureFilename = this.userAttachment.filename;
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

    submitChanges() {
        if (this.signUpForm.valid) {
            if (this.uploadedFile) {
                // SEND IT SOMEWHERE AND UPDATE USER
                this.userAttachment = this.createAttachment();
                this.uploadFile();
            } else {
                this.updateUser();
            }
        } else {
            this.errorMessage = 'Είναι απαραίτητο να συμπληρώσετε το όνομα και το επίθετό σας στα ελληνικά.';
            this.showSpinner = false;
        }
    }

    uploadFile() {
        this.errorMessage = '';
        this.showSpinner = true;
        this.userService.uploadSignature<string>(this.authService.getUserProp('email'), this.uploadedFile)
            .subscribe(
                event => {
                    // console.log('uploadAttachment responded: ', JSON.stringify(event));
                    if (event.type === HttpEventType.UploadProgress) {
                        console.log('uploadAttachment responded: ', event);
                    } else if ( event instanceof HttpResponse) {
                        console.log('file url is:', event.body);
                        this.userAttachment.url = event.body;
                    }
                },
                error => {
                    console.log(error);
                    this.errorMessage = 'Παρουσιάστηκε πρόβλημα κατά την αποθήκευση των αλλαγών';
                    this.showSpinner = false;
                },
                () => {
                    console.log('ready to update Request');
                    this.updateUser();
                }
            );
    }

    updateUser() {
      this.errorMessage = '';
      this.showSpinner = true;

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
              if ( !isNullOrUndefined(sessionStorage.getItem('state.location')) &&
                  (sessionStorage.getItem('state.location') !== '/sign-up') ) {
                  const state = sessionStorage.getItem('state.location');
                  sessionStorage.removeItem('state.location');
                  console.log('in sign-up returning to', state);
                  this.router.navigate([state]);
              } else {
                  this.router.navigate(['/home']);
              }
          }
      );
    }


    getUploadedFile(file: File) {
        this.uploadedFile = file;
    }

    linkToFile() {
        if (this.userAttachment && this.userAttachment.url) {
            window.open(this.userAttachment.url, '_blank', 'enabledstatus=0,toolbar=0,menubar=0,location=0');
        }
    }

}
