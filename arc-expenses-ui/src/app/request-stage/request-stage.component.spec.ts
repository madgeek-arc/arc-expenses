import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestStageComponent } from './request-stage.component';

describe('RequestStageComponent', () => {
  let component: RequestStageComponent;
  let fixture: ComponentFixture<RequestStageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RequestStageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RequestStageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
