import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Pattern } from 'src/app/models/pattern';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-beat-spec-detail',
  templateUrl: './beat-spec-detail.component.html',
  styleUrls: ['./beat-spec-detail.component.css']
})
export class BeatSpecDetailComponent implements OnInit {

  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  songId!: number;

  @Input()
  pattern!: Pattern;

  @Output()
  instruments!: Instrument[]

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  onInstrumentSelected(instrument: Instrument) {
    this.instrumentSelectEvent.emit(instrument)
  }

  onChannelChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_CHANNEL, event.target.value).subscribe(data => this.pattern = data)
  }

  onPresetChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_PRESET, event.target.value).subscribe(data => this.pattern = data)
  }

  onActiveChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_ACTIVE, event.target.value).subscribe(data => this.pattern = data)
  }

  onRootNoteChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_ROOT_NOTE, event.target.value).subscribe(data => this.pattern = data)
  }

  onDeviceChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_DEVICE, event.target.value).subscribe(data => this.pattern = data)
  }

  onDirectionChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_DIRECTION, event.target.value).subscribe(data => this.pattern = data)
  }

  onGateChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_GATE, event.target.value).subscribe(data => this.pattern = data)
  }

  onLastStepChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_LAST_STEP, event.target.value).subscribe(data => this.pattern = data)
  }

  onLengthChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_LENGTH, event.target.value).subscribe(data => this.pattern = data)
  }

  onProbablityChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_PROBABILITY, event.target.value).subscribe(data => this.pattern = data)
  }

  onRandomChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_RANDOM, event.target.value).subscribe(data => this.pattern = data)
  }

  onRepeatsChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_REPEATS, event.target.value).subscribe(data => this.pattern = data)
  }

  onSwingChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_SWING, event.target.value).subscribe(data => this.pattern = data)
  }

  onTransposeChange(event: { target: any; }) {
    this.midiService.updatePattern(this.pattern.id, Constants.PATTERN_UPDATE_TRANSPOSE, event.target.value).subscribe(data => this.pattern = data)
  }


  ngOnInit(): void {
    // this.uiService.addListener(this)
    this.midiService.allInstruments().subscribe(data => {
      this.instruments = this.uiService.sortByName(data)
    })
  }
}
