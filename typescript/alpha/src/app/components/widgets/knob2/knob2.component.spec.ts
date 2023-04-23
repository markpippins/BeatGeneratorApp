import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Knob2Component } from './knob2.component';

describe('Knob2Component', () => {
  let component: Knob2Component;
  let fixture: ComponentFixture<Knob2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ Knob2Component ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Knob2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
