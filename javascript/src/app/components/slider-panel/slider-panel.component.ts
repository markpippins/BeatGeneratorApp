import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Instrument} from "../../models/instrument";
import {MidiService} from "../../services/midi.service";
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css']
})
export class SliderPanelComponent implements OnInit, Listener {
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

  constructor(private midiService: MidiService, private uiService: UiService) {

  }

  onNotify(_messageType: number, _message: string) {

  }

  onSelect(selectedChannel: number) {
    this.instrument = undefined
    this.channel = selectedChannel;
    this.midiService
      .instrumentInfoByChannel(selectedChannel - 1)
      .subscribe(async (data) => {
        this.instrument = data;
        this.channelSelectEvent.emit(selectedChannel);
      });
  }

  ngOnInit(): void {
    this.uiService.addListener(this)
    this.onSelect(10);
    this.midiService
      .allInstruments()
      .subscribe(async (data) => {
        this.instruments = this.uiService.sortByName(data)
      });
  }

  configBtnClicked() {
    this.configModeOn = !this.configModeOn
  }
}
