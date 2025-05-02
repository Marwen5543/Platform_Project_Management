import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CalanderComponent } from './calander.component';

describe('CalanderComponent', () => {
  let component: CalanderComponent;
  let fixture: ComponentFixture<CalanderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalanderComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CalanderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
