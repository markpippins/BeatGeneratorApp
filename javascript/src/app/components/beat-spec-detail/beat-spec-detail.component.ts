import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
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

  ngOnInit(): void {
    // this.uiService.addListener(this)
    this.midiService.allInstruments().subscribe(data => {
      this.instruments = data
    })
  }
}
