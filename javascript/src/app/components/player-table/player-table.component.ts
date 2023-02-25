import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Player} from "../../models/player";
import {MidiService} from "../../services/midi.service";
import {Strike} from "../../models/strike";

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css']
})
export class PlayerTableComponent {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Input()
  players!: Player[]
  playerCols: string[] = [
    // 'add',
    // 'remove',
    // 'mute',
    'id',
    'Instrument',
    'Channel',
    'Preset',
    'Note',
    // 'operator',
    // 'comparison',
    // 'value',
    'Prob.',
    'Min V',
    'Max V',
  ];

  constructor(private midiService: MidiService) { }
  onRowClick(player: Player, $event: MouseEvent) {
    this.playerSelectEvent.emit(player);
  }

  onBtnClick(player: Player, action: string) {
    console.log(player.id)
    switch (action) {
      case 'add': { this.midiService.addPlayerClicked().subscribe(); break}
      case 'remove': { this.midiService.removePlayerClicked(player).subscribe(); break}
    }
  }

  onPass(player: Player) {
    this.playerSelectEvent.emit(player);
  }

}
