import {
  AfterContentChecked,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Listener } from 'src/app/models/listener';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';
import { Step } from '../../models/step';

interface PitchPair {
  midi: number;
  note: string;
}

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css'],
})
export class BeatSpecPanelComponent
  implements Listener, OnInit, AfterContentChecked
{
  flag: boolean = false;

  @Output()
  paramBtnClickEvent = new EventEmitter<number>();

  @Output()
  changeEvent = new EventEmitter<Step>();

  @Input()
  step!: Step;

  @Input()
  page!: number;

  @Output()
  active: boolean = false;

  pitchMap: PitchPair[] = [];

  constructor(private uiService: UiService, private midiService: MidiService) {
    uiService.addListener(this);
  }

  ngAfterContentChecked(): void {
    this.flag = (this.step != undefined && this.step.active)
  }

  ngOnInit(): void {
    for (let i = 0; i < 126; i++)
      this.pitchMap.push({
        midi: i,
        note: this.uiService.getNoteForValue(i, Constants.SCALE_NOTES),
      });
  }

  onNotify(messageType: number, _message: string, messageValue: number) {
    this.active =
      messageType == Constants.BEAT_DIV &&
      messageValue == this.step.position + 1;
  }

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.step.position);
    this.uiService.notifyAll(Constants.STEP_UPDATED, 'Step Updated', 0);
  }

  onLaneBtnClick() {
    this.step.active = !this.step.active;
    this.midiService
      .updateStep(
        this.step.id,
        this.step.page,
        this.step.position,
        Constants.STEP_ACTIVE,
        1
      )
      .subscribe(async (data) => (this.step = data));

    let element = document.getElementById('beat-btn-' + this.step.position);
    if (this.uiService.hasClass(element, 'active')) {
      this.uiService.removeClass(element, 'active');
      this.uiService.addClass(element, 'inactive');
    } else if (this.uiService.hasClass(element, 'inactive'))
      this.uiService.removeClass(element, 'inactive');
    this.uiService.addClass(element, 'active');
  }

  onNoteChange(step: Step, event: { target: any }) {
    if (step.pitch == event.target.value)
      this.midiService
        .updateStep(
          step.id,
          step.page,
          step.position,
          Constants.STEP_PITCH,
          event.target.value
        )
        .subscribe((data) => (this.step = data));
  }

  onVelocityChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.page,
        step.position,
        Constants.STEP_VELOCITY,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onGateChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.page,
        step.position,
        Constants.STEP_GATE,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onProbabilityChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.page,
        step.position,
        Constants.STEP_PROBABILITY,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onChange() {
    this.changeEvent.emit(this.step);
  }
}
