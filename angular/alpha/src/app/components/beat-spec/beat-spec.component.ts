import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Listener } from 'src/app/models/listener';
import { Pattern } from 'src/app/models/pattern';
import { PatternUpdateType } from 'src/app/models/pattern-update-type';
import { Song } from 'src/app/models/song';
import { UiService } from 'src/app/services/ui.service';
import { MidiService } from '../../services/midi.service';
import { Step } from 'src/app/models/step';
import { Swirl } from 'src/app/models/swirl';

@Component({
  selector: 'app-beat-spec',
  templateUrl: './beat-spec.component.html',
  styleUrls: ['./beat-spec.component.css'],
})
export class BeatSpecComponent implements OnInit, Listener {
  editStep: number | undefined;
  tickerId!: number;
  song!: Song;
  rows: string[][] = [
    [
      'Ride',
      'Clap',
      'Perc',
      'Bass',
      'Tom',
      'Clap',
      'Wood',
      'P1',
      'Ride',
      'fx',
      'Perc',
      'Bass',
      'Kick',
      'Snare',
      'Closed Hat',
      'Open Hat',
    ],
  ];

  pulse = 0;

  swirls = new Swirl<boolean>([
    true,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
  ]);
  // @Input()
  instruments!: Instrument[];

  constructor(private midiService: MidiService, private uiService: UiService) {
    uiService.addListener(this);
  }

  ngOnInit(): void {
    this.midiService.allInstruments().subscribe((instruments) => {
      this.instruments = this.uiService.sortByName(instruments);
      this.updateDisplay();
    });
  }

  paramsBtnClicked(step: number) {
    this.editStep = step;
  }

  forward = true;
  count = 0;

  onNotify(messageType: number, _message: string, messageValue: number) {
    if (messageType == Constants.TICKER_SELECTED) this.tickerId = messageValue;

    if (messageType == Constants.TICKER_CONNECTED) {
      this.pulse++;

      if (this.pulse % 8 == 0) {
        this.count++;

        if (this.forward) this.swirls.forward();
        else this.swirls.reverse();

        if (this.count % 15 == 0) this.forward = !this.forward;
      }
    }
  }

  updateDisplay(): void {
    this.midiService.songInfo().subscribe((data) => {
      this.song = data;
      this.song.patterns = this.uiService.sortByPosition(this.song.patterns)
      this.song.patterns.forEach(
        (p) => (p.steps = this.uiService.sortByPosition(p.steps))
      );
    });
  }

  onInstrumentSelected(instrument: Instrument, pattern: Pattern) {
    this.midiService
      .updatePattern(
        pattern.id,
        PatternUpdateType.INSTRUMENT,
        instrument.channel
      )
      .subscribe((data) => (this.song.patterns[pattern.position] = data));
  }

  selectedIndexChange(index: number) {
    this.uiService.notifyAll(
      Constants.INSTRUMENT_SELECTED,
      '',
      this.song.patterns[index].instrument?.id
    );
  }

  onStepChanged(_step: any) {
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }

  getInstrumentForStep(_pattern: Pattern, _step: Step): Instrument {
    let result = this.instruments?.filter(
      (i) => i.id == _pattern?.instrument?.id
    );
    return result[0];
  }

  getLabel(pattern: Pattern): string {
    let s = 'XOX ' + pattern.channel;
    if (pattern.name != undefined) s = pattern.name;

    if (s.toLowerCase() == 'microsoft gs wavetable synth') s = 'MS Wave';

    if (s.toLowerCase() == 'gervill') s = 'Gervill';

    return s + ' [' + pattern.position + ']';
  }
}
