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
import { Instrument } from 'src/app/models/instrument';
import { Pattern } from 'src/app/models/pattern';
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
  colors = ['violet', 'lightsalmon', 'lightseagreen', 'deepskyblue', 'fuchsia', 'mediumspringgreen', 'mediumpurple', 'firebrick', 'mediumorchid', 'aqua', 'olivedrab', 'cornflowerblue', 'lightcoral', 'crimson', 'goldenrod', 'tomato', 'blueviolet']

  @Output()
  paramBtnClickEvent = new EventEmitter<number>();

  @Output()
  changeEvent = new EventEmitter<Step>();

  @Input()
  pattern!: Pattern;

  @Input()
  swirling = true;

  @Input()
  step!: Step;

  @Input()
  instrument!: Instrument;

  @Output()
  active: boolean = false;

  pitchMap: PitchPair[] = [];

  constructor(private uiService: UiService, private midiService: MidiService) {
    uiService.addListener(this);
  }

  ngAfterContentChecked(): void {
    // this.selected = this.step != undefined && this.step.active;
  }

  ngOnInit(): void {
    for (let i = 0; i < 126; i++)
      this.pitchMap.push({
        midi: i,
        note: this.uiService.getNoteForValue(i, Constants.SCALE_NOTES),
      });
  }

  lastBeat = 0;

  onPadPressed(_index: number) {
    this.step.active = !this.step.active;
    // this.selected = !this.selected
  }


  onNotify(messageType: number, _message: string, _messageValue: any) {
    if (messageType == Constants.TICKER_STARTED) {
      this.lastBeat = 0;
      this.swirling = false;
    }

    if (messageType == Constants.TICKER_STOPPED) {
      this.lastBeat = 0;
      this.swirling = true;
    }

    if (messageType == Constants.BEAT_DIV) {
      if (this.lastBeat > this.pattern.lastStep)
        this.lastBeat = this.pattern.firstStep;
      else if (this.lastBeat == 0) this.lastBeat = 1;
      else
        this.active =
          // this.pattern.position == status.position &&
          this.lastBeat == this.step.position;
      this.lastBeat++;
    }
    // if (messageType == Constants.NOTIFY_SONG_STATUS) {
    //   let status: PatternStatus = messageValue;
    //   this.active =
    //     this.pattern.position == status.position &&
    //     status.activeStep == this.step.position;
    // }
  }

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.step.position);
    this.uiService.notifyAll(Constants.STEP_UPDATED, 'Step Updated', 0);
  }

  onLaneBtnClick() {
    this.step.active = !this.step.active;
    this.midiService
      .updateStep(this.step.id, this.step.position, Constants.STEP_ACTIVE, 1)
      .subscribe(async (data) => (this.step = data));

    let element = document.getElementById('beat-btn-' + this.step.position);
    if (this.uiService.hasClass(element, 'active')) {
      this.uiService.removeClass(element, 'active');
      this.uiService.addClass(element, 'inactive');
    } else if (this.uiService.hasClass(element, 'inactive'))
      this.uiService.removeClass(element, 'inactive');
    this.uiService.addClass(element, 'active');
  }

  onNoteChange(_event: { target: any }) {
    // alert(this.step.pitch)
    // if (this.step.pitch == event.target.value)
      this.midiService
        .updateStep(
          this.step.id,
          this.step.position,
          Constants.STEP_PITCH,
          this.step.pitch
        )
        .subscribe((data) => (this.step = data));
  }

  onVelocityChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
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
        step.position,
        Constants.STEP_PROBABILITY,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onChange() {
    this.changeEvent.emit(this.step);
  }

  getClass() {
    return this.active ? 'lane-808 ready' : 'lane-808';
  }

  getCaption(): string {
    return this.step.position.toString();
  }

  getMaxValue() {
    return 127
  }

  getMinValue() {
    return 0
  }

  getRangeColor() {
    return 'slategrey';
  }

  getStrokeWidth() {
    return 10;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'white';
  }


  getValueColor(index: number) {
    return this.colors[index];
  }

  getValueTemplate(name: string): string {
    return name
  }
}
