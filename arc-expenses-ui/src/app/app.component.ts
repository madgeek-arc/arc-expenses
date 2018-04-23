import { Component } from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import * as util from 'util';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'app';

  constructor(public router: Router) {}

  showTop() {
    return this.router.url !== '/sign-up';
  }
}
