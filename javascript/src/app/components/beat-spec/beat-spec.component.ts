import {Component, OnInit} from '@angular/core';
import {Step} from "../../models/step";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-beat-spec',
  templateUrl: './beat-spec.component.html',
  styleUrls: ['./beat-spec.component.css']
})
export class BeatSpecComponent implements OnInit {
  editStep: number | undefined
  stepCount: number = 16
  steps: Step[] = []
  pages = [0, 1, 2, 3, 4, 5, 6, 7]

  rows: string[][] = [[
    'Ride', 'Clap', 'Perc', 'Bass', 'Tom', 'Clap', 'Wood', 'P1',
    'Ride', 'fx', 'Perc', 'Bass',
    'Kick', 'Snare', 'Closed Hat', 'Open Hat',
  ]];

  paramsBtnClicked(step: number) {
    this.editStep = step
  }

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    // for (let page = 0; page < 8; page++)
      for (let step = 0; step < this.stepCount; step++) {
      this.steps.push({
        id: 0, position: step, active: false, gate: 50, pitch: 60, probability: 100, velocity: 110,
        page: 0, songId: 0
      })
    }
  }

  onStepChanged(step: Step) {
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }
}
