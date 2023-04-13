import {
  AfterContentChecked,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { MidiService } from '../../services/midi.service';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';
import { Listener } from 'src/app/models/listener';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';
import { TickerStatus } from 'src/app/models/ticker-status';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css'],
})
export class StatusPanelComponent
  implements OnInit, Listener, AfterContentChecked
{
  ppqSelectionIndex!: number;
  ppqs = [
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 18, 21, 23, 24, 27,
    29, 32, 33, 36, 37, 40, 42, 44, 46, 47, 48, 62, 64, 72, 74, 77, 84, 87, 88,
    89, 96,
  ];

  statusColumns = [
    'Ticker',
    'Tick',
    'Beat',
    'Bar',
    '',
    'PPQ',
    'BPM',
    'Beats / Bar',
    'Part Length',
    'Max',
  ];

  interval: number = 10;

  @Input()
  running = false;

  @Input()
  status!: TickerStatus;

  connected = false;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  constructor(private midiService: MidiService, private uiService: UiService) {}

  onNotify(_messageType: number, _message: string) {}

  ngOnInit(): void {
    this.updateDisplay();
  }

  ngAfterContentChecked(): void {}

  getBeats() {
    const beats = [];
    for (let i = this.status.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  waiting = false;
  nextCalled = false;

  lastBeat = 0;
  lastBar = 0;
  lastPart = 0;

  // if (this.waiting)
  //   return

  // if (this.status != undefined && this.status.id == 0)
  //   this.midiService.next(0)

  // this.waiting = true
  // if (this.connected)

  // let disconnected = !this.connected
  // if (disconnected && !this.nextCalled) {
  //   this.nextCalled = true
  //   this.midiService.next(0).subscribe(async (_newTicker) => {
  //     this.connected = true
  // //     this.status = data2
  //     this.uiService.notifyAll(Constants.CONNECTED, '', 1)
  //     this.waiting = false
  //   })
  // }
  // await this.midiService.delay(this.status?.playing ? 100 : 250);
  // this.waiting = false;

  updateDisplay(): void {
    this.midiService.tickerStatus().subscribe(
      async (data) => {
        this.status = data;
        this.setIndexForPPQ();
        await this.midiService.delay(this.status?.playing ? 200 : 1000);
        this.updateDisplay();
      }

      // async (err) => {
      //   console.log(err);
      //   this.connected = false;
      //   this.uiService.notifyAll(Constants.DISCONNECTED, '', 0);
      //   await this.midiService.delay(500);
      //   this.waiting = false;
      //   this.updateDisplay();
      // },

      // async () => {
      //   await this.midiService.delay(500);
      //   this.waiting = false;
      //   this.updateDisplay();
      // }
    );

    if (this.status != undefined) {
      if (this.status.beat != this.lastBeat) {
        this.lastBeat = this.status.beat;
        this.uiService.notifyAll(Constants.BEAT_DIV, '', this.status.beat);
      }
      if (this.status.bar != this.lastBar) {
        this.lastBar = this.status.bar;
        this.uiService.notifyAll(Constants.BAR_DIV, '', this.status.bar);
      }
      if (this.status.part != this.lastPart) {
        this.lastPart = this.status.part;
        this.uiService.notifyAll(Constants.PART_DIV, '', this.status.part);
      }
    }
  }

  onTempoChange(event: { target: any }) {
    this.midiService
      .updateTicker(this.status.id, TickerUpdateType.BPM, event.target.value)
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Tempo changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onBeatsPerBarChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.status.id,
        TickerUpdateType.BEATS_PER_BAR,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'BPM changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onBarsChange(event: { target: any }) {
    this.midiService
      .updateTicker(this.status.id, TickerUpdateType.BARS, event.target.value)
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Bars changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartsChange(event: { target: any }) {
    this.midiService
      .updateTicker(this.status.id, TickerUpdateType.PARTS, event.target.value)
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Parts changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartLengthChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.status.id,
        TickerUpdateType.PART_LENGTH,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(
          Constants.TICKER_UPDATED,
          'Part Length changed',
          0
        )
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPPQSelectionChange() {
    this.midiService
      .updateTicker(
        this.status.id,
        TickerUpdateType.PPQ,
        this.ppqs[this.ppqSelectionIndex]
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'PPQ changed', 0)
      );
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex]);
  }

  setIndexForPPQ() {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.status.ticksPerBeat);
  }

  getTickerPosition() {
    return Math.round(this.status.beat);
  }
}
