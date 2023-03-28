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
  ppqs = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 21, 23, 24, 27, 32, 36, 33, 37, 40, 42, 44, 46, 48, 64, 72, 84, 88, 96]

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
        await this.midiService.delay(this.connected && this.ticker != undefined && this.ticker.playing ? 200 : 200);
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
    this.midiService.updateTicker(this.ticker.id, Constants.BPM, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBeatsPerBarChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.BEATS_PER_BAR, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBarsChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.BARS, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPartsChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.PARTS, event.target.value).subscribe()
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

  getRounded(value: number) {
    return Math.round(this.ticker.beat)
  }
}
