import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.scss']
})
export class RequestsComponent implements OnInit {

  title = 'Υπάρχοντα Αιτήματα';
  constructor() { }

  ngOnInit() {
  }

}
