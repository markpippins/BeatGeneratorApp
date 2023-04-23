import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstrumentSelectDialogComponent } from './instrument-select-dialog.component';

describe('InstrumentSelectDialogComponent', () => {
  let component: InstrumentSelectDialogComponent;
  let fixture: ComponentFixture<InstrumentSelectDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InstrumentSelectDialogComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstrumentSelectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
