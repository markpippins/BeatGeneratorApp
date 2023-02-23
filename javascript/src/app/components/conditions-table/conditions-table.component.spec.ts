import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConditionsTableComponent } from './conditions-table.component';

describe('ConditionsTableComponent', () => {
  let component: ConditionsTableComponent;
  let fixture: ComponentFixture<ConditionsTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConditionsTableComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConditionsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
