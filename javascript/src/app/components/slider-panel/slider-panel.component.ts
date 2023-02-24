import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Instrument} from "../../models/instrument";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css']
})
export class SliderPanelComponent implements OnInit {
  @Input()
  channel = 10;

  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Input()
  instrument?: Instrument | undefined;

  constructor(private midiService: MidiService) {}
  onSelect(selectedChannel: number) {
    this.channel = selectedChannel;
    this.midiService
      .instrumentInfo(selectedChannel - 1)
      .subscribe(async (data) => {
        this.instrument = data;
        this.channelSelectEvent.emit(selectedChannel);
      });
  }
  ngOnInit(): void {
    this.onSelect(this.channel);
  }
}
