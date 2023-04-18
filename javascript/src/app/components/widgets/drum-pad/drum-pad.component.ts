import { Component, Input } from '@angular/core';
import { Instrument } from '../../../models/instrument';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css'],
})
export class DrumPadComponent {
  constructor(private uiService: UiService) {}

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
    this.active = true;

    let element = document.getElementById('pad-ring-' + this.index);
    if (element != undefined)
      this.uiService.swapClass(element, 'standby', 'enabled');

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
    let result = this.active ? 'pad-ring onstep' : 'pad-ring';

    return result;
  }
}
