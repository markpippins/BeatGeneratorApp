import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Player } from 'src/app/models/player';
import { PlayerUpdateType } from 'src/app/models/player-update-type';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-strike-detail',
  templateUrl: './strike-detail.component.html',
  styleUrls: ['./strike-detail.component.css']
})
export class StrikeDetailComponent {

  @Output()
  changeEvent = new EventEmitter<Player>();

  constructor(private midiService: MidiService, private uiService: UiService) {
  }
  @Input()
  player!: Player


  onLevelChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, PlayerUpdateType.LEVEL, event.target.value).subscribe(data => this.player = data)
  }

  onMinVelocityChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, PlayerUpdateType.MIN_VELOCITY, event.target.value).subscribe()
  }

  onMaxVelocityChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, PlayerUpdateType.MAX_VELOCITY, event.target.value).subscribe()
  }
}
