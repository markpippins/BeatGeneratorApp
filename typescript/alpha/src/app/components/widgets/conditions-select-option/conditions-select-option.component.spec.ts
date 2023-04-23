import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConditionsSelectOptionComponent } from './conditions-select-option.component';

describe('ConditionsSelectOptionComponent', () => {
  let component: ConditionsSelectOptionComponent;
  let fixture: ComponentFixture<ConditionsSelectOptionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConditionsSelectOptionComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConditionsSelectOptionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
