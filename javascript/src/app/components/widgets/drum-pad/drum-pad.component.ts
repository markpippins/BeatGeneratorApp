import {Component, Input} from '@angular/core';
import {Instrument} from "../../../models/instrument";
import {MidiService} from "../../../services/midi.service";

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css']
})
export class DrumPadComponent {
  constructor(private midiService: MidiService) {}

  @Input()
  name!: string;

  @Input()
  index!: number;

  @Input()
  note!: number;

  @Input()
  instrument!: Instrument;

  @Input()
  channel!: number;
  padPressed() {
    this.midiService.playNote(
      this.instrument.name,
      this.channel - 1,
      this.note
    );
  }
}
