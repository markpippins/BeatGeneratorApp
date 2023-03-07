import {Component, EventEmitter, Input, Output} from '@angular/core';
import {StepData} from "../../models/step-data";

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css']
})
export class BeatSpecPanelComponent {

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
    this.toggleClass(element, 'inactive', 'active')

    element = document.getElementById("beat-led-" + this.stepData.step)
    this.toggleClass(element, 'inactive', 'active')
  }

  toggleClass(el: any, classNameA: string, classNameB: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, classNameB);
    } else if (el.className.indexOf(classNameB) >= 0) {
      el.className = el.className.replace(classNameB, classNameA);
    }
  }

  onChange() {
    this.changeEvent.emit(this.stepData)
  }

  onNoteScroll($event: Event) {
    alert($event)
  }
}
