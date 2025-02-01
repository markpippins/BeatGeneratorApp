import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { Player } from '../models/player';
import { Ticker } from '../models/ticker';
import { MidiService } from './midi.service';

@Injectable({
  providedIn: 'root'
})
export class TickerService {

  players: Player[] = []

  ticker: Ticker = {
    bar: 0,
    beat: 0,
    beatDivider: 0,
    beatsPerBar: 0,
    done: false,
    id: 0,
    maxTracks: 0,
    partLength: 0,
    playing: false,
    songLength: 0,
    stopped: false,
    swing: 0,
    tempoInBPM: 0,
    tick: 0,
    ticksPerBeat: 0,
    activePlayerIds: [],
    part: 0,
    bars: 0,
    parts: 0,
    tickCount: 0,
    barCount: 0,
    beats: 0,
    beatCount: 0,
    partCount: 0,
    noteOffset: 0,
    players: []
  }

  constructor(private zone: NgZone, private midiService: MidiService) { }

  getTicker(): Ticker {
    return this.ticker
  }

  forward() {
      if (this.ticker.id > 0 && this.ticker.playing) {
        // this.consoleOutput.pop()
        // this.consoleOutput.push('ticker is currently playing')
      } else this.midiService.next(this.ticker.id).subscribe(async (data) => {
        // this.clear();
        this.ticker = data
        return data
      })
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
              // this.cycleColors();
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
}
