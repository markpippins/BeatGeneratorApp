import { Component, Input } from '@angular/core';
import { Instrument } from 'src/app/models/instrument';

@Component({
  selector: 'app-midi-knob',
  templateUrl: './midi-knob.component.html',
  styleUrls: ['./midi-knob.component.css'],
})
export class MidiKnobComponent {
  @Input()
  name!: string;

  colors = ['violet', 'lightsalmon', 'lightseagreen', 'deepskyblue', 'fuchsia', 'mediumspringgreen', 'mediumpurple', 'firebrick', 'mediumorchid', 'aqua', 'olivedrab', 'cornflowerblue', 'lightcoral', 'crimson', 'goldenrod', 'tomato', 'blueviolet']
  @Input()
  instrument!: Instrument

  @Input()
  cc!: number;

  value: number = 50

  getMaxValue() {
    return 127
  }

  getMinValue() {
    return 0
  }

  getRangeColor() {
    return 'slategrey';
  }

  getStrokeWidth() {
    return 10;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'white';
  }

  @Input()
  index!: number

  getValueColor() {
    return this.colors[this.index];
  }

  getValueTemplate() {
    return this.name;
  }
}
