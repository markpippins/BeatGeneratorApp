import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PartStackerComponent } from './part-stacker.component';

describe('PartStackerComponent', () => {
  let component: PartStackerComponent;
  let fixture: ComponentFixture<PartStackerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PartStackerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PartStackerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
