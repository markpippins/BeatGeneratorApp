import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Instrument} from "../../models/instrument";
import {MidiService} from "../../services/midi.service";
import {MatCheckboxChange} from "@angular/material/checkbox";

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css']
})
export class SliderPanelComponent implements OnInit {
  @Input()
  channel = 1;

  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Input()
  instrument!: Instrument | undefined

  @Input()
  instruments!: Instrument[]

  @Output()
  configModeOn = false;

  constructor(private midiService: MidiService) {
  }

  onSelect(selectedChannel: number) {
    this.instrument = undefined
    this.channel = selectedChannel;
    this.midiService
      .instrumentInfo(selectedChannel - 1)
      .subscribe(async (data) => {
        this.instrument = data;
        this.channelSelectEvent.emit(selectedChannel);
      });
  }

  ngOnInit(): void {
    this.onSelect(10);
    this.midiService
      .allInstruments()
      .subscribe(async (data) => {
        this.instruments = data;
      });
  }

  change(event: MatCheckboxChange) {
    this.configModeOn = !this.configModeOn
  }
}
