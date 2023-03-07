import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core'
import {Instrument} from "../../../models/instrument"
import {MidiService} from "../../../services/midi.service";
import {Rule} from "../../../models/rule";
import {LookupItem} from "../../../models/lookup-item";

@Component({
  selector: 'app-instrument-selector',
  templateUrl: './instrument-selector.component.html',
  styleUrls: ['./instrument-selector.component.css']
})
export class InstrumentSelectorComponent implements OnInit {
  selectedInstrument: Instrument | undefined
  instruments!: LookupItem[]
  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  instrumentName!: string

  constructor(private midiService: MidiService) {
  }

  selectionChange(event: { target: any; }) {
    this.onInstrumentChanged(event.target.value);
  }

  onInstrumentChanged(instrument: Instrument) {
    // this.selectedInstrument = instrument;
    // this.midiService
    //   .instrumentInfo(this.channel - 1)
    //   .subscribe(async (data) => {
    //     this.instrument = data;
    //     this.channelSelectEvent.emit(this.channel);
    //     this.instrumentSelectEvent.emit(this.instrument);
    //   });
  }

  ngOnInit(): void {
    this.midiService.instrumentLookup().subscribe(async data => {
      this.instruments = data
    })
  }


  // onOperatorChange(rule: Rule, event: { target: any }) {
  //   let value = this.OPERATOR.indexOf(event.target.value)
  //   this.midiService.updateRule(this.player.id, rule.id, value, rule.comparisonId, rule.value).subscribe()
  //   rule.operatorId = value
  //   // let op = 'operatorSelect-' + rule.id
  //   this.setSelectValue(event.target, value)
  // }
}
