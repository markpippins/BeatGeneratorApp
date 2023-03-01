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
  isPlaying = false;
  constructor(private midiService: MidiService) {
  }

  onClick(action: string) {
    this.onActionSelected(action)
    this.clickEvent.emit(action)
  }

  onActionSelected(action: string) {
    switch (action) {
      case 'forward': {
        this.midiService.nextClicked().subscribe()
        // this.toolBarTransportButtonClicked('stop')
        // this.delay(3000)
        // this.toolBarTransportButtonClicked('play')
        break
      }
      case 'play': {
        this.midiService.startClicked().subscribe()
        this.isPlaying = true
        // this.isPlaying = true
        // this.updateDisplay()
        break
      }
      case 'stop': {
        this.midiService.stopClicked().subscribe()
        this.isPlaying = false
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'pause': {
        this.midiService.pauseClicked().subscribe()
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'record': {
        this.midiService.recordClicked().subscribe()
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'add': {
        this.midiService.addPlayerClicked().subscribe()
        break
      }

      case 'clear': {
        this.midiService.clearPlayers().subscribe()
        break
      }
    }
  }

}
