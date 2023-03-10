import {Component, EventEmitter, Input, Output} from '@angular/core';
import { UiService } from 'src/app/services/ui.service';
import {StepData} from "../../models/step-data";

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
  changeEvent = new EventEmitter<StepData>();

  @Input()
  stepData!: StepData

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.stepData.step)
  }

  onLaneBtnClick() {
    let element = document.getElementById("beat-btn-" + this.stepData.step)
    this.uiService.swapClass(element, 'inactive', 'active')

    element = document.getElementById("beat-led-" + this.stepData.step)
    this.uiService.swapClass(element, 'inactive', 'active')
  }

  onChange() {
    this.changeEvent.emit(this.stepData)
  }

  onNoteScroll($event: Event) {
    alert($event)
  }
}
