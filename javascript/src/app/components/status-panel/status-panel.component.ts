import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Ticker} from "../../models/ticker";
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';
import { Listener } from 'src/app/models/listener';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css']
})
export class StatusPanelComponent implements OnInit, Listener {

  ppqSelectionIndex !: number
  ppqs = [1, 2, 4, 8, 12, 24, 48, 96]

  statusColumns = ['Ticker', 'Tick', 'Beat', 'Bar', '', 'PPQ', 'BPM', 'Beats / Bar', 'Part Length', 'Max']

  @Input()
  running = false

  @Input()
  ticker!: Ticker;

  connected = false;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  onNotify(messageType: number, message: string) {

  }

  ngOnInit(): void {
    this.updateDisplay()
  }

  ngAfterContentChecked(): void {
      this.setIndexForPPQ()
  }

  getBeats() {
    const beats = [];
    for (let i = this.ticker.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  waiting = false;
  nextCalled = false

  updateDisplay(): void {
    if (this.waiting)
      return
    this.waiting = true
    this.midiService.tickerStatus().subscribe(
      async (data) => {
        let disconnected = !this.connected
        this.ticker = data;
        if (disconnected && !this.nextCalled) {
          this.nextCalled = true
          this.midiService.next(0).subscribe(async (data2) => {
            this.connected = true
            this.ticker = data2
            this.uiService.notifyAll(Constants.CONNECTED, '')
            this.waiting = false
          })
        }
        await this.midiService.delay(5000);
        this.waiting = false
        this.updateDisplay();
      },

      async err => {
        console.log(err)
        this.connected = false
        this.uiService.notifyAll(Constants.DISCONNECTED, '')
        await this.midiService.delay(50000);
        this.waiting = false
        this.updateDisplay();
      },

      async () => {
        await this.midiService.delay(50000);
        this.waiting = false
        this.updateDisplay();
    });
  }

  onTempoChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.PPQ, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBeatsPerBarChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.BEATS_PER_BAR, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPartLengthChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.PART_LENGTH, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPPQSelectionChange(data: any) {
    this.midiService.updateTicker(this.ticker.id, Constants.PPQ, this.ppqs[this.ppqSelectionIndex]).subscribe()
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex])
  }

  setIndexForPPQ() {
    // this.ppqs.filter(i => i.id == this.player.instrument.id).forEach(ins => {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.ticker.ticksPerBeat);
    // })
  }
}
