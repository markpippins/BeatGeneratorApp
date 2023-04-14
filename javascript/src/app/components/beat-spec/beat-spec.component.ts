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

  // @Input()
  instruments!: Instrument[];

  ngOnInit(): void {
    // this.uiService.addListener(this)
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
    });
    this.updateDisplay();
  }

  paramsBtnClicked(step: number) {
    this.editStep = step;
  }

  constructor(private midiService: MidiService, private uiService: UiService) {
    uiService.addListener(this);
  }
  onNotify(messageType: number, _message: string, messageValue: number) {
    if (messageType == Constants.TICKER_SELECTED) this.tickerId = messageValue;
  }

  updateDisplay(): void {
    this.midiService.songInfo().subscribe((data) => {
      this.song = data;
      // this.song.patterns = this.uiService.sortByPosition(this.song.patterns)
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

  onStepChanged(_step: any) {
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }

  getInstrumentForStep(_pattern: Pattern, _step: Step): Instrument {
    let result = this.instruments.filter(i => i.id == _pattern.instrumentId)
    return result[0]
  }

  getLabel(pattern: Pattern): string {
    let s = 'XOX ' + pattern.channel;
    if (pattern.name != undefined) s = pattern.name;

    if (s.toLowerCase() == 'microsoft gs wavetable synth') s = 'MS Wave';

    if (s.toLowerCase() == 'gervill') s = 'Gervill';

    return s + ' (' + pattern.position + ')';
  }
}
