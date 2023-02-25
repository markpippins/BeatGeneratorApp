import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Player} from "../../models/player";
import {Strike} from "../../models/strike";

@Component({
  selector: 'app-player-panel',
  templateUrl: './player-panel.component.html',
  styleUrls: ['./player-panel.component.css']
})
export class PlayerPanelComponent {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Input()
  players!: Player[]
  selectedPlayer!: Player;

  onPlayerSelected(player: Player) {
    this.playerSelectEvent.emit(player);
  }
}
