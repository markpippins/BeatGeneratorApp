import { Component, Input } from '@angular/core';
import { Instrument } from '../../../models/instrument';

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css'],
})
export class DrumPadComponent {
  constructor() {}

  @Input()
  name!: string;

  @Input()
  index!: number;

  @Input()
  note!: number;

  @Input()
  otherNote!: number;

  @Input()
  instrument!: Instrument;

  pressed = false;

  @Input()
  active: boolean = false;

  @Input()
  channel!: number;

  padPressed() {
    this.pressed = !this.pressed;
    // this.active = true;

    // let element = document.getElementById('pad-ring-' + this.index);
    // if (element != undefined)
    //   this.uiService.swapClass(element, 'standby', 'enabled');

    // this.midiService.playNote(
    //   this.instrument.name,
    //   this.channel - 1,
    //   this.note
    // );
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
