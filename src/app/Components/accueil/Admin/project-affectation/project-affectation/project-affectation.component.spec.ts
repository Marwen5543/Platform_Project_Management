import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectAffectationComponent } from './project-affectation.component';

describe('ProjectAffectationComponent', () => {
  let component: ProjectAffectationComponent;
  let fixture: ComponentFixture<ProjectAffectationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectAffectationComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProjectAffectationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
