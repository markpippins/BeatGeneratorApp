import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Player} from "../../models/player";
import {MidiService} from "../../services/midi.service";
import {Strike} from "../../models/strike";
import { Instrument } from 'src/app/models/instrument';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css']
})
export class PlayerTableComponent {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  selectedPlayer: Player | undefined;

  INSTRUMENT = 0;
  NOTE = 1;
  PROBABILITY = 2;
  MIN_VELOCITY = 3;
  MAX_VELOCITY = 4;
  MUTE = 5;

  @Input()
  players!: Player[]
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
        this.midiService.addPlayer().subscribe(async (data) => {
          this.players.push(data);
        });
        break
      }
      case 'remove': {
        this.midiService.removePlayer(player).subscribe(async (data) => {
          this.players = data;
          if (this.players.length == 0)
            this.selectedPlayer = {
              allowedControlMessages: [],
              channel: 0,
              id: 0,
              instrumentId: 0,
              maxVelocity: 0,
              minVelocity: 0,
              note: 0,
              preset: 0,
              probability: 0,
              rules: []
            }
        });
        this.players = this.players.filter(p => p.id != player.id)
        break
      }
    }
  }


  initBtnClicked() {
    this.onBtnClick({
      allowedControlMessages: [],
      channel: 0,
      id: 0,
      instrumentId: 0,
      maxVelocity: 0,
      minVelocity: 0,
      note: 0,
      preset: 0,
      probability: 0,
      rules: []
    }, 'add')
  }

  noteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, this.NOTE, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined)
      this.playerSelectEvent.emit(player);
  }

  onInstrumentSelected(player: Player) {
    this.midiService.updatePlayer(player.id, this.INSTRUMENT, player.instrumentId).subscribe()
  }

  toggleClass(el: any, className: string) {
    // if (el.className.indexOf(className) >= 0) {
    //   el.className = el.className.replace(className, "");
    // } else {
    //   el.className += className;
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
