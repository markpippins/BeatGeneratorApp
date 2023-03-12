import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Ticker} from "../../models/ticker";
import { Constants } from 'src/app/models/constants';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css']
})
export class StatusPanelComponent implements OnInit {

  ppqSelectionIndex !: number
  ppqs = [1, 2, 4, 8, 12, 24, 48, 96]

  statusColumns = ['Ticker', 'Tick', 'Beat', 'Bar', '', 'PPQ', 'BPM', 'Beats / Bar', 'Part Length', 'Max']

  @Input()
  running = false

  @Input()
  ticker!: Ticker;

  connected = false;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.updateDisplay()
  }

  ngAfterContentChecked(): void {
      this.setIndexForPPQ()
  }

  getBeats() {
    const beats = [];
    for (let i = this.ticker.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  updateDisplay(): void {
    this.midiService.tickerStatus().subscribe(async (data) => {
      this.connected = true
      this.ticker = data;
      await this.midiService.delay(this.ticker == undefined ? 5000 : this.connected && this.ticker.playing ? 50: 1000);
      this.updateDisplay();
    }, err => {
        console.log(err)
        this.connected = false
        this.midiService.delay(5000);
        this.updateDisplay();
      },
    () => {
      // console.log('call complete')
    });
  }

  onTempoChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.PPQ, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onBeatsPerBarChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.BEATS_PER_BAR, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPartLengthChange(event: { target: any; }) {
    this.midiService.updateTicker(this.ticker.id, Constants.PART_LENGTH, event.target.value).subscribe()
    this.tempoChangeEvent.emit(event.target.value)
  }

  onPPQSelectionChange(data: any) {
    alert(this.ppqSelectionIndex)
    this.midiService.updateTicker(this.ticker.id, Constants.PPQ, this.ppqs[this.ppqSelectionIndex]).subscribe()
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex])
  }

  setIndexForPPQ() {
    // this.ppqs.filter(i => i.id == this.player.instrument.id).forEach(ins => {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.ticker.ticksPerBeat);
    // })
  }
}
