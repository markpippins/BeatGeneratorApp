import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Ticker} from "../../models/ticker";
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';
import { Listener } from 'src/app/models/listener';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css']
})
export class StatusPanelComponent implements OnInit, Listener {

  ppqSelectionIndex !: number
  ppqs = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 18, 21, 23, 24, 27, 29, 32, 33, 36, 37, 40, 42, 44, 46, 47, 48, 62, 64, 72, 74, 77, 84, 87, 88, 89, 96]

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

  lastBeat = 0

  updateDisplay(): void {
    if (this.waiting)
      return
    if (this.ticker != undefined && this.ticker.id == 0)
      this.midiService.next(0)

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
            this.uiService.notifyAll(Constants.CONNECTED, '', 1)
            this.waiting = false
          })
        }
        await this.midiService.delay(this.ticker?.playing ? 250 : 750);
        this.waiting = false

        this.updateDisplay();
      },

      async err => {
        console.log(err)
        this.connected = false
        this.uiService.notifyAll(Constants.DISCONNECTED, '', 0)
        await this.midiService.delay(500);
        this.waiting = false
        this.updateDisplay();
      },

    //   async () => {
    //     await this.midiService.delay(500);
    //     this.waiting = false
    //     this.updateDisplay();
    // }
    );

    if (this.ticker != undefined && this.ticker.beat != this.lastBeat) {
      this.lastBeat = this.ticker.beat
      this.uiService.notifyAll(Constants.BEAT_DIV, '', this.ticker.beat)
    }

  }

  onTempoChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.BPM, event.target.value).subscribe(data =>  this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Tempo changed', 0))
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBeatsPerBarChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.BEATS_PER_BAR, event.target.value).subscribe(data => this.uiService.notifyAll(Constants.TICKER_UPDATED, 'BPM changed', 0))
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBarsChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.BARS, event.target.value).subscribe(data => this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Bars changed', 0))
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPartsChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.PARTS, event.target.value).subscribe(data => this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Parts changed', 0))
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPartLengthChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.PART_LENGTH, event.target.value).subscribe(data => this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Part Length changed', 0))
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPPQSelectionChange(data: any) {
    this.midiService.updateTicker(this.ticker.id, TickerUpdateType.PPQ, this.ppqs[this.ppqSelectionIndex]).subscribe(data => this.uiService.notifyAll(Constants.TICKER_UPDATED, 'PPQ changed', 0))
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex])
  }

  setIndexForPPQ() {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.ticker.ticksPerBeat);
  }

  getRounded(value: number) {
    return Math.round(this.ticker.beat)
  }
}
