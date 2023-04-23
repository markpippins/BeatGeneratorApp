import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TickerAdjustComponent } from './ticker-adjust.component';

describe('TickerAdjustComponent', () => {
  let component: TickerAdjustComponent;
  let fixture: ComponentFixture<TickerAdjustComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TickerAdjustComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TickerAdjustComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
