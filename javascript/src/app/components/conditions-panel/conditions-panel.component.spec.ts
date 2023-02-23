import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConditionsPanelComponent } from './conditions-panel.component';

describe('ConditionsPanelComponent', () => {
  let component: ConditionsPanelComponent;
  let fixture: ComponentFixture<ConditionsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConditionsPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConditionsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
