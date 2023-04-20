import {
  Component,
  EventEmitter,
  Input,
  NgZone,
  OnInit,
  Output,
} from '@angular/core';
import { MidiService } from '../../services/midi.service';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';
import { TickerStatus } from 'src/app/models/ticker-status';
import { Observable, Subscription } from 'rxjs';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css'],
})
export class StatusPanelComponent implements OnInit {
  sub!: Subscription;
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

  @Input()
  status!: TickerStatus;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  constructor(
    private zone: NgZone,
    private midiService: MidiService,
    private uiService: UiService
  ) {}

  getMessages(): Observable<string> {
    return Observable.create(
      (observer: {
        next: (arg0: any) => void;
        error: (arg0: Event) => void;
      }) => {
        let source = new EventSource('http://localhost:8080/api/tick');
        source.onmessage = (event) => {
          this.zone.run(() => {
            observer.next(event.data);
          });
        };

        source.onerror = (event) => {
          this.zone.run(() => {
            observer.error(event);
          });
        };
      }
    );
  }

  ngOnInit(): void {
    this.sub = this.getMessages().subscribe({
      next: (data: string) => {
        this.status = JSON.parse(data);
        this.updateDisplay();
      },
      error: (err: any) => console.error(err),
    });
  }

  ngOnDestroy(): void {
    this.sub && this.sub.unsubscribe();
  }

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

  updateDisplay(): void {
    if (this.status != undefined) {
      if (this.status.beat != this.lastBeat) {
        this.lastBeat = this.status.beat;
        this.uiService.notifyAll(Constants.BEAT_DIV, '', this.status.beat);
        this.status.patternStatuses.forEach((patternStatus) =>
          this.uiService.notifyAll(
            Constants.NOTIFY_SONG_STATUS,
            '',
            patternStatus
          )
        );
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
