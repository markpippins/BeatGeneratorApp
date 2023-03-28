import {Component, OnInit} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Listener } from 'src/app/models/listener';
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
  stepCount: number = 16
  steps: Step[][] = []
  pages = [0, 1, 2, 3, 4, 5, 6, 7]
  tickerId!: number
  rows: string[][] = [[
    'Ride', 'Clap', 'Perc', 'Bass', 'Tom', 'Clap', 'Wood', 'P1',
    'Ride', 'fx', 'Perc', 'Bass',
    'Kick', 'Snare', 'Closed Hat', 'Open Hat',
  ]];

  paramsBtnClicked(step: number) {
    this.editStep = step
  }

  constructor(private midiService: MidiService, uiService: UiService) {
    uiService.addListener(this)
  }
  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType == Constants.TICKER_SELECTED)
      this.tickerId = messageValue
  }

  ngOnInit(): void {
    for (let page = 0; page < 8; page++) {
      this.steps.push([])
      for (let index =1; index < this.stepCount + 1; index++) {
        this.steps[page].push({
          id: 0, position: index, active: false, gate: 50, pitch: 60, probability: 100, velocity: 110,
          page: page, songId: 0,
          channel: 0
        })
      }
    }
  }

  onInstrumentSelected(instrument: Instrument, page: number) {
    this.steps[page].forEach(s => s.channel = instrument.channel)
    this.steps[page].forEach(s => {
      s.channel = instrument.channel;
      this.midiService.updateStep(s.id, s.page, s.position, Constants.STEP_ACTIVE, 1)
        .subscribe(data => this.steps[page][this.steps[page].indexOf(s)] = data)
    })
  }

  onStepChanged(step: any) {
    // alert(step)
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }
}
