import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesNavComponent } from './rules-nav.component';

describe('RulesNavComponent', () => {
  let component: RulesNavComponent;
  let fixture: ComponentFixture<RulesNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RulesNavComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RulesNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
