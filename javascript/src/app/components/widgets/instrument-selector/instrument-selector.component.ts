import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core'
import {Instrument} from "../../../models/instrument"
import {MidiService} from "../../../services/midi.service";
import {Rule} from "../../../models/rule";
import {LookupItem} from "../../../models/lookup-item";
import { Player } from 'src/app/models/player';

@Component({
  selector: 'app-instrument-selector',
  templateUrl: './instrument-selector.component.html',
  styleUrls: ['./instrument-selector.component.css']
})
export class InstrumentSelectorComponent implements OnInit {


  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  instruments!: Instrument[]

  selectedInstrumentId !: number

  @Input()
  player!: Player

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.midiService.allInstruments().subscribe(async data => {
      this.instruments = data
    })
  }

  onSelectionChange(data: any) {
    // alert("selected --->"+this.instruments[this.selectedInstrumentId].id+' '+this.instruments[this.selectedInstrumentId].name);
    this.instrumentSelectEvent.emit(this.instruments[this.selectedInstrumentId])
  }
}
