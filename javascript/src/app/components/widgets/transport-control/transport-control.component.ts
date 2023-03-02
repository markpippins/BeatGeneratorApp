import {Component, EventEmitter, Output} from '@angular/core'
import {MidiService} from "../../../services/midi.service"

@Component({
  selector: 'app-transport-control',
  templateUrl: './transport-control.component.html',
  styleUrls: ['./transport-control.component.css']
})
export class TransportControlComponent {
  @Output()
  clickEvent = new EventEmitter<string>()
  onClick(action: string) {
    this.clickEvent.emit(action)
  }
}
