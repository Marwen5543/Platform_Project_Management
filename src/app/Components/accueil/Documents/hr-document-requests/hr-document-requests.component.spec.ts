import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HrDocumentRequestsComponent } from './hr-document-requests.component';

describe('HrDocumentRequestsComponent', () => {
  let component: HrDocumentRequestsComponent;
  let fixture: ComponentFixture<HrDocumentRequestsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HrDocumentRequestsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HrDocumentRequestsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
