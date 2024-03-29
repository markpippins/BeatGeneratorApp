import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlayerNavComponent } from './player-nav.component';

describe('PlayerNavComponent', () => {
  let component: PlayerNavComponent;
  let fixture: ComponentFixture<PlayerNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PlayerNavComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlayerNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
