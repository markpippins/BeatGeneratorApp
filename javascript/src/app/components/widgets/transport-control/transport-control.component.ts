import {Component, EventEmitter, Output} from '@angular/core'
import { Constants } from 'src/app/models/constants'
import { Listener } from 'src/app/models/listener'
import { UiService } from 'src/app/services/ui.service'

@Component({
  selector: 'app-transport-control',
  templateUrl: './transport-control.component.html',
  styleUrls: ['./transport-control.component.css']
})
export class TransportControlComponent implements Listener {

  constructor(private uiService: UiService) {
    this.uiService.addListener(this)
  }

  @Output()
  clickEvent = new EventEmitter<string>()

  connected = false;

  onClick(action: string) {
    this.clickEvent.emit(action)
  }

  onNotify(_messageType: number, _message: string) {
    if (_messageType == Constants.CONNECTED)
      this.connected = true
    else if (_messageType == Constants.DISCONNECTED)
      this.connected = false
  }
}
