import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Ticker} from "../../models/ticker";

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css']
})
export class StatusPanelComponent implements OnInit {

  statusColumns = ['Ticker', 'Tick', 'Beat', 'Bar', '', 'PPQ', 'BPM', 'Beats / Bar', 'Part Length', 'Max']

  @Input()
  running = false

  @Input()
  ticker!: Ticker;

  @Output()
  clickEvent = new EventEmitter<string>();

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.updateDisplay()
  }

  getBeats() {
    const beats = [];
    for (let i = this.ticker.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  updateDisplay(): void {
    this.midiService.tickerStatus().subscribe(async (data) => {
      this.ticker = data;
      await this.midiService.delay(this.ticker == undefined ? 5000 : this.ticker.playing ? 50: 1000);
      this.updateDisplay();
    });
  }

  onClick(action: string) {
    this.clickEvent.emit(action);
  }
}
