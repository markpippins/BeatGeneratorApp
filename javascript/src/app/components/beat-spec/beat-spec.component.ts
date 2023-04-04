import {Component, OnInit} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Listener } from 'src/app/models/listener';
import { Pattern } from 'src/app/models/pattern';
import { PatternUpdateType } from 'src/app/models/pattern-update-type';
import { Song } from 'src/app/models/song';
import { UiService } from 'src/app/services/ui.service';
import {Step} from "../../models/step";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-beat-spec',
  templateUrl: './beat-spec.component.html',
  styleUrls: ['./beat-spec.component.css']
})
export class BeatSpecComponent implements OnInit, Listener {
  editStep: number | undefined
  // stepCount: number = 16
  // steps: Step[][] = []
  // pages = [0, 1, 2, 3, 4, 5, 6, 7]
  tickerId!: number
  song!: Song
  rows: string[][] = [[
    'Ride', 'Clap', 'Perc', 'Bass', 'Tom', 'Clap', 'Wood', 'P1',
    'Ride', 'fx', 'Perc', 'Bass',
    'Kick', 'Snare', 'Closed Hat', 'Open Hat',
  ]];

  paramsBtnClicked(step: number) {
    this.editStep = step
  }

  constructor(private midiService: MidiService, private uiService: UiService) {
    uiService.addListener(this)
  }
  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType == Constants.TICKER_SELECTED)
      this.tickerId = messageValue
  }

  ngOnInit(): void {
    this.updateDisplay();
  }

  updateDisplay(): void {
    this.midiService.songInfo().subscribe(data => {
        this.song = data
        // this.song.patterns = this.uiService.sortByPosition(this.song.patterns)
        this.song.patterns.forEach(p => p.steps = this.uiService.sortByPosition(p.steps))
      })
  }

  onInstrumentSelected(instrument: Instrument, pattern: Pattern) {

    this.midiService.updatePattern(pattern.id, PatternUpdateType.INSTRUMENT, instrument.channel)
      .subscribe(data => this.song.patterns[pattern.position] = data)
  }

  onStepChanged(step: any) {
    // alert(step)
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }

  getLabel(pattern: Pattern): string {
    let s = "XOX " + pattern.channel
    if (pattern.name != undefined)
      s = pattern.name

    if (s == "microsoft gs wavetable synth")
      s = "MS Wave"

      if (s == "gervill")
      s = "Gervill"


    return s;
  }
}
