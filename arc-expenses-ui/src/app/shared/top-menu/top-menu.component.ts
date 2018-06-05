import { Component, OnInit } from '@angular/core';
import {AuthenticationService} from '../../services/authentication.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ManageRequestsService } from '../../services/manage-requests.service';
import { ContactUsMail } from '../../domain/operation';


declare const UIkit: any;

@Component({
  selector: 'app-top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.scss']
})
export class TopMenuComponent implements OnInit {

  loggedIn: boolean = false;

  contactForm: FormGroup;
  modalError: string;
  showSpinner: boolean;

  constructor(private authService: AuthenticationService,
              private requestService: ManageRequestsService,
              private fb: FormBuilder) { }

  ngOnInit() {
    this.isUserLoggedIn();
  }

  login() {
    if (!this.isUserLoggedIn()) {
        this.authService.loginWithState();
    }
  }

  logout() {
    this.authService.logout();
  }

  isUserLoggedIn() {
    return this.authService.getIsUserLoggedIn();
  }

  getUserName() {
    return this.authService.getUserFirstName() + ' ' + this.authService.getUserLastName();
  }

  onClick(id: string) {
      const el: HTMLElement = document.getElementById(id);
      el.classList.remove('uk-open');
  }

  showContactForm() {
    this.contactForm = this.fb.group({
        name: ['', Validators.required],
        surname: ['', Validators.required],
        email: ['', [Validators.required, Validators.email] ],
        subject: ['', Validators.required],
        message: ['', Validators.required]
    });

    if (this.isUserLoggedIn()) {
      this.contactForm.get('name').setValue(this.authService.getUserFirstName());
      this.contactForm.get('surname').setValue(this.authService.getUserLastName());
      this.contactForm.get('email').setValue(this.authService.getUserEmail());
    }
    UIkit.modal('#contactModal').show();
  }

  submitContactForm(event: any) {
    this.modalError = '';

    if (this.contactForm.valid) {
        this.showSpinner = true;

        const emailParams = new ContactUsMail();
        emailParams.name = this.contactForm.get('name').value;
        emailParams.surname = this.contactForm.get('surname').value;
        emailParams.email = this.contactForm.get('email').value;
        emailParams.subject = this.contactForm.get('subject').value;
        emailParams.message = this.contactForm.get('message').value;

        // send email somehow
        this.requestService.sendContactFormToService(emailParams).subscribe(
            res => console.log(res),
            error => {
                console.log(error);
                this.showSpinner = false;
                this.modalError = 'Το μήνυμα δεν εστάλη. Παρακαλώ προσπαθήστε ξανά.';
              },
            () => {
              this.modalError = '';
              this.showSpinner = false;
              UIkit.modal('#contactModal').hide();
            }
        );
    } else {
      this.modalError = 'Όλα τα πεδία είναι υποχρεωτικά.';
    }

  }

}
