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
// import { SongStatus } from 'src/app/models/song-status';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css'],
})
export class StatusPanelComponent implements OnInit {
  tickerSubscription!: Subscription;
  // songSubscription!: Subscription;
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
  tickerStatus!: TickerStatus;

  // @Input()
  // songStatus!: SongStatus;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  colors = ['red', 'orange', 'yellow', 'green', 'blue', 'indigo', 'violet'];
  index = 0;

  constructor(
    private zone: NgZone,
    private midiService: MidiService,
    private uiService: UiService
  ) {}

  private pulse = 0;

  cycleColors() {
    let colorContainer = document.getElementById('dashboard') as HTMLElement;
    if (colorContainer) {
      colorContainer.style.backgroundColor = this.colors[this.index];
      this.index = (this.index + 1) % this.colors.length;
    }
  }

  getTickerMessages(): Observable<string> {
    return Observable.create(
      (observer: {
        next: (arg0: any) => void;
        error: (arg0: Event) => void;
      }) => {
        let source = new EventSource('http://localhost:8080/api/tick');
        source.onmessage = (event) => {
          this.zone.run(() => {
            // console.log('tick');
            // console.log(event.data);
            this.cycleColors();
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

  // getSongMessages(): Observable<string> {
  //   return Observable.create(
  //     (observer: {
  //       next: (arg0: any) => void;
  //       error: (arg0: Event) => void;
  //     }) => {
  //       let source = new EventSource('http://localhost:8080/api/xox');
  //       source.onmessage = (event) => {
  //         this.zone.run(() => {
  //           console.log('song');
  //           console.log(event.data);
  //           observer.next(event.data);
  //         });
  //       };

  //       source.onerror = (event) => {
  //         this.zone.run(() => {
  //           observer.error(event);
  //         });
  //       };
  //     }
  //   );
  // }

  ngOnInit(): void {
    this.tickerSubscription = this.getTickerMessages().subscribe({
      next: (data: string) => {
        this.pulse++;
        this.uiService.notifyAll(Constants.TICKER_CONNECTED, '', this.pulse);
        this.tickerStatus = JSON.parse(data);
        this.updateDisplay();
      },
      error: (err: any) => console.error(err),
    });

    // this.songSubscription = this.getSongMessages().subscribe({
    //   next: (data: string) => {
    //     this.songStatus = JSON.parse(data);
    //     this.updateDisplay();
    //   },
    //   error: (err: any) => console.error(err),
    // });
  }

  ngOnDestroy(): void {
    this.tickerSubscription && this.tickerSubscription.unsubscribe();
    // this.songSubscription && this.songSubscription.unsubscribe();
  }

  getBeats() {
    const beats = [];
    for (let i = this.tickerStatus.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  waiting = false;
  nextCalled = false;

  lastBeat = 0;
  lastBar = 0;
  lastPart = 0;

  updateDisplay(): void {
    if (this.tickerStatus != undefined) {
      this.tickerStatus.patternStatuses.forEach((patternStatus) =>
        this.uiService.notifyAll(
          Constants.NOTIFY_SONG_STATUS,
          '',
          patternStatus
        )
      );

      if (this.tickerStatus.beat != this.lastBeat) {
        this.lastBeat = this.tickerStatus.beat;
        this.uiService.notifyAll(
          Constants.BEAT_DIV,
          '',
          this.tickerStatus.beat
        );

        if (this.tickerStatus.bar != this.lastBar) {
          this.lastBar = this.tickerStatus.bar;
          this.uiService.notifyAll(
            Constants.BAR_DIV,
            '',
            this.tickerStatus.bar
          );
        }

        if (this.tickerStatus.part != this.lastPart) {
          this.lastPart = this.tickerStatus.part;
          this.uiService.notifyAll(
            Constants.PART_DIV,
            '',
            this.tickerStatus.part
          );
        }
      }
    }
  }

  onTempoChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.BPM,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Tempo changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onBeatsPerBarChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
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
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.BARS,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Bars changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartsChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.PARTS,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Parts changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartLengthChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
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
        this.tickerStatus.id,
        TickerUpdateType.PPQ,
        this.ppqs[this.ppqSelectionIndex]
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'PPQ changed', 0)
      );
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex]);
  }

  setIndexForPPQ() {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.tickerStatus.ticksPerBeat);
  }

  getTickerPosition() {
    return Math.round(this.tickerStatus.beat);
  }
}
