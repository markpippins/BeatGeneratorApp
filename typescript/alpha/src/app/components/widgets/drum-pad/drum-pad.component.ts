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
  selector: boolean = false;

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
  muted: boolean = false;

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
    if (this.selector)
      return 'none'

    let result = this.active ? 'pad-ring-active' : 'pad-ring';

    return result;
  }

  getIndicatorClass(_index: number): string {
    if (this.selector)
      return 'pad-15'

    let result = 'pad-indicator-content'
    switch (_index) {
      case 0: {
        result += this.pressed? ' armed' :' ready';
        break;
      }
      case 1: {
        result += this.muted ? ' muted' : ' active';
        break;
      }

      case 2: {
        result += ' pad-2';
        break;
      }

      case 2: {
        result += ' pad-2';
        break;
      }

      case 3: {
        result += ' pad-3';
        break;
      }
      case 4: {
        result += ' pad-4';
        break;
      }
      case 5: {
        result += ' pad-5';
        break;
      }
      case 6: {
        result += ' pad-6';
        break;
      }
      case 7: {
        result += ' pad-7';
        break;
      }
      case 8: {
        result += ' pad-8';
        break;
      }
    }


    return result;
  }
}
