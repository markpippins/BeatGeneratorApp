import { Component } from '@angular/core';
import { MidiMessage } from 'src/app/models/midi-message';
import { MidiService } from 'src/app/services/midi.service';

@Component({
  selector: 'app-launchpad',
  templateUrl: './launchpad.component.html',
  styleUrls: ['./launchpad.component.css'],
})
export class LaunchpadComponent {
  constructor(private midiService: MidiService) {}

  onClick(note: number) {
    this.midiService.sendMessage(MidiMessage.NOTE_ON, 11, note, 120);
    this.midiService.sendMessage(MidiMessage.NOTE_OFF, 11, note, 120);
  }
}
