import {Component, EventEmitter, Input, Output} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';
import {Step} from "../../models/step";

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css']
})
export class BeatSpecPanelComponent {

  constructor(private uiService: UiService, private midiService: MidiService) {}

  @Output()
  paramBtnClickEvent = new EventEmitter<number>();

  @Output()
  changeEvent = new EventEmitter<Step>();

  @Input()
  stepData!: Step

  @Input()
  page!: number

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.stepData.position)
    this.uiService.notifyAll(Constants.STEP_UPDATED, this.stepData.position.toString())
  }

  onLaneBtnClick() {
    this.stepData.active = !this.stepData.active
    this.midiService.updateStep(this.stepData.id, Constants.STEP_ACTIVE, 1).subscribe(async data => this.stepData = data)

    let element = document.getElementById("beat-btn-" + this.stepData.position)
    this.uiService.swapClass(element, 'inactive', 'active')

    element = document.getElementById("beat-led-" + this.stepData.position)
    this.uiService.swapClass(element, 'inactive', 'active')

  }

  onNoteChange(stepData: Step, event: { target: any; }) {
    this.midiService.updateStep(stepData.id, Constants.STEP_PITCH, event.target.value).subscribe(data => this.stepData = data)
  }

  onVelocityChange(stepData: Step, event: { target: any; }) {
    this.midiService.updateStep(stepData.id, Constants.STEP_VELOCITY, event.target.value).subscribe(data => this.stepData = data)
  }

  onGateChange(stepData: Step, event: { target: any; }) {
    this.midiService.updateStep(stepData.id, Constants.STEP_GATE, event.target.value).subscribe(data => this.stepData = data)
  }

  onProbabilityChange(stepData: Step, event: { target: any; }) {
    this.midiService.updateStep(stepData.id, Constants.STEP_PROBABILITY, event.target.value).subscribe(data => this.stepData = data)
  }

  onChange() {
    this.changeEvent.emit(this.stepData)
  }

  onNoteScroll($event: Event) {
    alert($event)
  }
}
