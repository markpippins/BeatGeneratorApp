import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Instrument } from '../../../models/instrument';

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css'],
})

export class DrumPadComponent {

  @Output()
  padPressedEvent = new EventEmitter<number>();

  @Input()
  name!: string;

  @Input()
  caption!: string;

  @Input()
  index!: number;

  @Input()
  note!: number;

  @Input()
  otherNote!: number;

  @Input()
  instrument!: Instrument;

  @Input()
  pressed!: boolean;

  @Input()
  active: boolean = false;

  @Input()
  channel!: number;

  constructor() {}

  padPressed() {
    this.padPressedEvent.emit(this.index)
  }

  getPadClass(): string {
    let result = this.pressed
      ? 'pad pad-' + this.index + ' pressed'
      : 'pad pad-' + this.index;

    return result;
  }

  getPadRingClass(): string {
    let result = this.active ? 'pad-ring' : 'pad-ring';

    return result;
  }

  getIndicatorClass(_index: number): string {
    let result = 'pad-indicator-content'
    switch (_index) {
      case 0: {
        result += this.pressed? ' armed' :' ready';
        break;
      }
      case 1: {
        result += this.active ? ' onstep' : ' offstep';
        break;
      }
    }

    return result;
  }
}
