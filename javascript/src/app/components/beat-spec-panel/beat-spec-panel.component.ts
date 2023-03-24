import {Component, EventEmitter, Input, Output} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';
import {Step} from "../../models/step";

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css']
})
export class BeatSpecPanelComponent {

  constructor(private uiService: UiService) {}

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
    let element = document.getElementById("beat-btn-" + this.stepData.position)
    this.uiService.swapClass(element, 'inactive', 'active')

    element = document.getElementById("beat-led-" + this.stepData.position)
    this.uiService.swapClass(element, 'inactive', 'active')
  }

  onChange() {
    this.changeEvent.emit(this.stepData)
  }

  onNoteScroll($event: Event) {
    alert($event)
  }
}
