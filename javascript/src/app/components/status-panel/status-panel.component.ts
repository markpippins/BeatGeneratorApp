import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Ticker} from "../../models/ticker";

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css']
})
export class StatusPanelComponent implements OnInit {

  statusColumns = ['Tick', 'Beat', 'Bar', 'Bar Length', 'Beats / Bar', 'Part Length', 'Delay', 'Max']

  @Output()
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
    this.midiService.tickerInfo().subscribe(async (data) => {
      this.ticker = data;
      await this.midiService.delay(this.ticker == undefined ? 5000 : 250);
      this.updateDisplay();
    });
  }

  onClick(action: string) {
    this.clickEvent.emit(action);
  }
}
