import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core'
import {Instrument} from "../../../models/instrument"
import {MidiService} from "../../../services/midi.service";
import {Rule} from "../../../models/rule";
import {LookupItem} from "../../../models/lookup-item";
import { Player } from 'src/app/models/player';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-instrument-selector',
  templateUrl: './instrument-selector.component.html',
  styleUrls: ['./instrument-selector.component.css']
})
export class InstrumentSelectorComponent implements OnInit {



  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  instruments!: Instrument[]

  @Output()
  selectionIndex !: number

  @Input()
  player!: Player

  constructor(private midiService: MidiService, private uiService: UiService) {}

  ngOnInit(): void {
    this.midiService.allInstruments().subscribe(async data => {
      this.instruments = data
    })
  }

  ngAfterContentChecked(): void {
    if (this.instruments != undefined && this.player != undefined) {
      let sel  = 'player_instrument-' + this.player.id
      this.setIndexForInstrument()
    }
  }

  onSelectionChange(data: any) {
    // alert("selected --->"+this.instruments[this.selectedInstrumentId].id+' '+this.instruments[this.selectedInstrumentId].name);
    this.instrumentSelectEvent.emit(this.instruments[this.selectionIndex])
  }

  setIndexForInstrument() {
    this.instruments.filter(i => i.id == this.player.instrument.id).forEach(ins => {
      this.selectionIndex = this.instruments.indexOf(ins);
    })
  }
}
