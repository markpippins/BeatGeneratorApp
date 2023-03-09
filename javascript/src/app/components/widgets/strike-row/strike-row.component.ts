import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Player } from 'src/app/models/player';
import { MidiService } from 'src/app/services/midi.service';

@Component({
  selector: 'app-strike-row',
  templateUrl: './strike-row.component.html',
  styleUrls: ['./strike-row.component.css']
})
export class StrikeRowComponent {

  @Input()
  player!: Player

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Output()
  playerAddedEvent = new EventEmitter()

  @Output()
  playerRemovedEvent = new EventEmitter<Player>()

  playerCols: string[] = [
    // 'add',
    // 'remove',
    // 'mute',
    'ID',
    'InstrumentId',
    'Ch',
    'Device',
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

  // onRowClick(player: Player, event: MouseEvent) {
  //   let element = document.getElementById("player-row-" + player.id)
  //   if (this.selectedPlayer == undefined) {
  //     this.selectedPlayer = player
  //     this.playerSelectEvent.emit(player);
  //     this.toggleClass(element, 'selected')
  //   } else {
  //     this.selectedPlayer = undefined
  //     this.toggleClass(element, 'active-table-row')
  //   }
  // }

  onBtnClick(player: Player, action: string) {
    switch (action) {
      case 'add': {
        this.midiService.addPlayer().subscribe(async (data) => {
          this.playerAddedEvent.emit(data)
        });
        break
      }
      case 'remove': {
        this.playerRemovedEvent.emit(this.player)
        break
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick(this.player, 'add')
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrument = instrument
    this.midiService.updatePlayer(player.id, Constants.INSTRUMENT, instrument.id).subscribe()
  }

  onNoteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.NOTE, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined)
      this.playerSelectEvent.emit(player);
  }

  toggleClass(el: any, className: string) {
    // if (el.className.indexOf(className) >= 0) {
    //   el.className = el.className.replace(className, "");
    // } else {
    //   el.className += className;
    // }
  }

  onRowClick(player: Player, event: MouseEvent) {
    // let element = document.getElementById("player-row-" + player.id)
    // if (this.selectedPlayer == undefined) {
    //   this.selectedPlayer = player
    //   this.playerSelectEvent.emit(player);
    //   this.toggleClass(element, 'selected')
    // } else {
    //   this.selectedPlayer = undefined
    //   this.toggleClass(element, 'active-table-row')
    // }
  }

  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id)
    if (element != null) { // @ts-ignore
      element.selectedIndex = val
    }
  }
}
