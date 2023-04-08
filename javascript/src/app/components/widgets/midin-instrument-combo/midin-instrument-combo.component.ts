import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-midin-instrument-combo',
  templateUrl: './midin-instrument-combo.component.html',
  styleUrls: ['./midin-instrument-combo.component.css']
})
export class MidinInstrumentComboComponent  implements OnInit {

  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  instruments!: Instrument[]

  @Output()
  selectionIndex !: number

  @Output()
  selectedInstrumentId !: number

  @Input()
  identifier!: string


  constructor(private uiService: UiService) {}

  ngOnInit(): void {

  }

  ngAfterContentChecked(): void {
    if (this.instruments != undefined && this.identifier != undefined) {
      this.setIndexForInstrument()
    }
  }

  onSelectionChange() {
    this.instrumentSelectEvent.emit(this.instruments[this.selectionIndex])
    this.uiService.notifyAll(Constants.STATUS, this.instruments[this.selectionIndex].name + ' selected.', 0)
  }

  setIndexForInstrument() {
    this.instruments.filter(i => i.id == this.selectedInstrumentId).forEach(ins => {
      this.selectionIndex = this.instruments.indexOf(ins);
    })
  }

}
