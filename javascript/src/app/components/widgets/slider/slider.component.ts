import {Component, Input} from '@angular/core';
import {Options} from "@angular-slider/ngx-slider";
import '@angular/animations';
import {MidiService} from "../../../services/midi.service";
import {MidiMessage} from "../../../models/midi-message";

@Component({
  selector: 'app-slider',
  templateUrl: './slider.component.html',
  styleUrls: ['./slider.component.css']
})
export class SliderComponent {
  @Input()
  channel!: number;

  @Input()
  cc!: number;

  @Input()
  label!: string;
  value: number = 0;
  options: Options = {
    floor: 1,
    ceil: 127,
    vertical: true,
    hideLimitLabels: true,
  };

  constructor(private midiService: MidiService) {
  }
  onChange(element: SliderComponent, event: Event) {
    this.midiService.sendMessage(MidiMessage.CONTROL_CHANGE, this.channel, this.cc, this.value)
  }
}
