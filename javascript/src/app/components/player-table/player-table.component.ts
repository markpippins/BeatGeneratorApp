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

  selectedPlayer: Player | undefined;

  @Input()
  players!: Player[]
  playerCols: string[] = [
    // 'add',
    // 'remove',
    // 'mute',
    // 'id',
    'Ch',
    'Strike',
    'Pre',
    'Note',
    // 'operator',
    // 'comparison',
    // 'value',
    'Prob.',
    'Min V',
    'Max V',
  ];

  constructor(private midiService: MidiService) {
  }

  onRowClick(player: Player, event: MouseEvent) {
    let element = document.getElementById("player-row-" + player.id)
    if (this.selectedPlayer == undefined) {
      this.selectedPlayer = player
      this.playerSelectEvent.emit(player);
      this.toggleClass(element, 'selected')
    } else {
      this.selectedPlayer = undefined
      this.toggleClass(element, 'active-table-row')
    }
  }

  onBtnClick(player: Player, action: string) {
    switch (action) {
      case 'add': {
        this.midiService.addPlayerClicked().subscribe(async (data) => {
          this.players.push(data);
        });
        break
      }
      case 'remove': {
        this.midiService.removePlayerClicked(player).subscribe(async (data) => {
          this.players = data;
        });
        this.players = this.players.filter(p => p.id != player.id)
        break
      }
    }
  }


  initBtnClicked() {
    this.midiService.addPlayerClicked().subscribe();
  }

  noteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayerClicked(player.id, 1, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined)
      this.playerSelectEvent.emit(player);
  }

  toggleClass(el: any, className: string) {
    if (el.className.indexOf(className) >= 0) {
      el.className = el.className.replace(className, "");
    } else {
      el.className += className;
    }
  }

  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id)
    if (element != null) { // @ts-ignore
      element.selectedIndex = val
    }
  }

}
