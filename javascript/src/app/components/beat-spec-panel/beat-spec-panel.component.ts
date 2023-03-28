import {Component, EventEmitter, Input, Output} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Listener } from 'src/app/models/listener';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';
import {Step} from "../../models/step";

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css']
})
export class BeatSpecPanelComponent implements Listener {

  constructor(private uiService: UiService, private midiService: MidiService) {
    uiService.addListener(this)
  }

  @Output()
  paramBtnClickEvent = new EventEmitter<number>();

  @Output()
  changeEvent = new EventEmitter<Step>();

  @Input()
  step!: Step

  @Input()
  page!: number

  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType == Constants.BEAT_DIV && messageValue == this.step.position) {
      let element = document.getElementById("beat-btn-" + this.step.position)
      this.uiService.swapClass(element, 'inactive', 'active')

      // element = document.getElementById("beat-led-" + this.step.position)
      // this.uiService.swapClass(element, 'inactive', 'active')
    }
  }

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.step.position)
    this.uiService.notifyAll(Constants.STEP_UPDATED, "Step Updated", 0)
  }

  onLaneBtnClick() {
    this.step.active = !this.step.active
    this.midiService.updateStep(this.step.id, this.step.page, this.step.position, Constants.STEP_ACTIVE, 1).subscribe(async data => this.step = data)

    let element = document.getElementById("beat-btn-" + this.step.position)
    this.uiService.swapClass(element, 'inactive', 'active')

    // element = document.getElementById("beat-led-" + this.step.position)
    // this.uiService.swapClass(element, 'inactive', 'active')

  }

  onNoteChange(step: Step, event: { target: any; }) {
    this.midiService.updateStep(step.id, step.page, step.position, Constants.STEP_PITCH, event.target.value).subscribe(data => this.step = data)
  }

  onVelocityChange(step: Step, event: { target: any; }) {
    this.midiService.updateStep(step.id, step.page, step.position, Constants.STEP_VELOCITY, event.target.value).subscribe(data => this.step = data)
  }

  onGateChange(step: Step, event: { target: any; }) {
    this.midiService.updateStep(step.id, step.page, step.position, Constants.STEP_GATE, event.target.value).subscribe(data => this.step = data)
  }

  onProbabilityChange(step: Step, event: { target: any; }) {
    this.midiService.updateStep(step.id, step.page, step.position, Constants.STEP_PROBABILITY, event.target.value).subscribe(data => this.step = data)
  }

  onChange() {
    this.changeEvent.emit(this.step)
  }

  onNoteScroll($event: Event) {
    alert($event)
  }
}
