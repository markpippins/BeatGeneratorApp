import {Component, OnInit} from '@angular/core';
import {StepData} from "../../models/step-data";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-beat-spec',
  templateUrl: './beat-spec.component.html',
  styleUrls: ['./beat-spec.component.css']
})
export class BeatSpecComponent implements OnInit {
  editStep: number | undefined
  stepCount: number = 16
  steps: StepData[] = []

  paramsBtnClicked(step: number) {
    this.editStep = step
  }

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    for (let step = 1; step < this.stepCount + 1; step++) {
      this.steps.push({step: step, active: false, gate: 50, pitch: 60, probability: 100, velocity: 110})
    }
  }

  onStepChanged(step: StepData) {
    this.midiService.addTrack(this.steps).subscribe()
  }
}
