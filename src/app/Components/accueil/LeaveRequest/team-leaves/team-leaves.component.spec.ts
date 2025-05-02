import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TeamLeavesComponent } from './team-leaves.component';

describe('TeamLeavesComponent', () => {
  let component: TeamLeavesComponent;
  let fixture: ComponentFixture<TeamLeavesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamLeavesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TeamLeavesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
