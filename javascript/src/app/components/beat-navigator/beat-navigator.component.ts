import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Listener } from 'src/app/models/listener';
import { Player } from 'src/app/models/player';
import { Ticker } from 'src/app/models/ticker';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-beat-navigator',
  templateUrl: './beat-navigator.component.html',
  styleUrls: ['./beat-navigator.component.css']
})
export class BeatNavigatorComponent implements OnInit, Listener {
  @Input()
  ticker!: Ticker

  window = 16
  ticksPosition = this.window
  tickRange: number[] = []
  tickOverflow: string[] = []
  ticks:  number[] = []
  beats:  number[] = []
  bars:   number[] = []
  parts:  number[] = []
  range:  number[] = []
  notes:  string[] = []

  selectedTicks:  boolean[] = []
  selectedBeats:  boolean[] = []
  selectedBars:   boolean[] = []
  selectedParts:  boolean[] = []
  selectedNote:   number = 0 
  resolution: string[] = ['tick', 'beat', 'bar']

  identifier = 1
  
  instruments!: Instrument[]

  constructor(private uiService: UiService, private midiService: MidiService) {
    uiService.addListener(this)
  }

  getBeatPerBar(): number {
    return this.ticker == undefined ? 4 : this.ticker.beatsPerBar
  }

  ngOnInit(): void {
    this.updateDisplay();
    this.midiService.allInstruments().subscribe(data => {
      this.instruments = this.uiService.sortByName(data)
    })
  }

  onInstrumentSelected(instrument: Instrument) {
    this.range = []
    if (instrument.lowestNote > 0 && instrument.highestNote > instrument.lowestNote)
    for (let note = instrument.lowestNote; note < instrument.highestNote + 1; note++) {
      this.range.push(note)
    }
  }


  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType == Constants.TICKER_UPDATED)
      this.updateDisplay()

      if (messageType == Constants.BEAT_DIV) {
        let element = document.getElementById("beat-btn-" + messageValue)
        this.uiService.swapClass(element, 'inactive', 'active')
      }
    }

  updateDisplay() {
    this.midiService.tickerInfo().subscribe(data => {
      this.ticksPosition = this.window

      this.ticker = data
      this.ticks = []
      this.beats = []
      this.bars = []
      this.parts = []
      this.selectedTicks = []
      this.selectedBeats = []
      this.selectedBars = []
      this.selectedParts = []

      this.tickRange = []
      this.tickOverflow = []
      this.notes = []

      for (let index = 0; index < this.ticker.ticksPerBeat; index++) {
        this.ticks.push(index + 1)
        this.notes.push(this.uiService.getNoteForValue(index + 1))
        this.selectedTicks.push(false)
        if (this.tickRange.length < this.window)
          this.tickRange.push(index)
      }

      while (this.tickRange.length + this.tickOverflow.length < this.window)
        this.tickOverflow.push('')    


      for (let index = 0; index < this.ticker.beatsPerBar; index++) {
        this.beats.push(index + 1)
        this.selectedBeats.push(false)
      }

      for (let index = 0; index < this.ticker.bars; index++) {
        this.bars.push(index + 1)
        this.selectedBars.push(false)
      }

      for (let index = 0; index < this.ticker.parts; index++) {
        this.parts.push(index + 1)
        this.selectedParts.push(false)
      }
    })
  }

  getTickRangeAsStrings() {
    return this.tickRange.map(tr => String(tr))
  }

  onTickForwardClicked() {
    if (this.tickRange[this.window - 1] == this.ticks[this.ticks.length - 1])
      return

    if (this.ticksPosition >= this.ticks.length)
      return

    this.tickRange = []
    this.tickOverflow = []

    // this.ticksPosition += this.window
    while (this.tickRange.length < this.window) {
      if (this.ticksPosition == this.ticks.length)
        break
      else this.tickRange.push(this.ticksPosition++)
    }

    while (this.tickRange.length + this.tickOverflow.length < this.window)
      this.tickOverflow.push('')    
  }

  onTickBackClicked() {
    if (this.ticksPosition == 0)
      return

    this.tickRange = []
    this.tickOverflow = []
    this.ticksPosition -= this.window * 2
    if (this.ticksPosition < 0)
        this.ticksPosition = 0
    
      while (this.ticksPosition < this.ticks.length && (this.tickOverflow.length + this.tickRange.length < this.window)) {
        while (this.ticksPosition == this.ticks.length)
          this.tickOverflow.push('')      
        this.tickRange.push(this.ticksPosition)
        this.ticksPosition++
      }
  
    while (this.tickRange.length + this.tickOverflow.length < this.window)
      this.tickOverflow.push('')    
     // this.uiService. mini-tick-btn-{{tick}}
}

  onTickClicked(tick: number, event: Event) {
    this.selectedTicks[tick] = !this.selectedTicks[tick] 
    this.uiService.swapClass(event.target, "mini-nav-btn-selected", "mini-nav-btn")
  }


  onBeatClicked(beat: number, event: Event) {
    this.selectedBeats[beat] = !this.selectedBeats[beat] 
    this.uiService.swapClass(event.target, "mini-nav-btn-selected", "mini-nav-btn")
  }

  onBarClicked(bar: number, event: Event) {
    this.selectedBars[bar] = !this.selectedBars[bar] 
    this.uiService.swapClass(event.target, "mini-nav-btn-selected", "mini-nav-btn")
  }

  onPartClicked(part: number, event: Event) {
    this.selectedParts[part] = !this.selectedParts[part] 
    this.uiService.swapClass(event.target, "mini-nav-btn-selected", "mini-nav-btn")
  }

  onNoteClicked(note: number, event: Event) {
    this.range.forEach(note => {
      let element = document.getElementById("mini-note-btn-" + this.selectedNote)
      if (element != undefined)
        this .uiService.removeClass(element, "-selected")
    })
    
    this.selectedNote = note
    this.uiService.swapClass(event.target, "mini-nav-btn-selected", "mini-nav-btn")
  }

}
