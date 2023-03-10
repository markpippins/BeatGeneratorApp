import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Player} from "../../models/player";
import {MidiService} from "../../services/midi.service";
import {Strike} from "../../models/strike";
import { Instrument } from 'src/app/models/instrument';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css']
})
export class PlayerTableComponent {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  selectedPlayer: Player | undefined;

  DUMMY_PLAYER: Player = {
    id: 0,
    maxVelocity: 0,
    minVelocity: 0,
    note: 0,
    preset: 0,
    probability: 0,
    rules: [],
    allowedControlMessages: [],
    instrument: {
      "id": 0,
      "name": "",
      "channel": 0,
      "lowestNote": 0,
      "highestNote": 0,
      "highestPreset": 0,
      "preferredPreset": 0,
      "assignments": new Map() ,
      "boundaries": new Map() ,
      "hasAssignments": false,
      "pads": 0,
      "controlCodes": []
    }
  }

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
    // 'ID',
    'Device',
    // 'Ch',
    'Preset',
    'Pitch',
    // 'operator',
    // 'comparison',
    // 'value',
    'Probability',
    'Min V',
    'Max V',
  ];

  constructor(private midiService: MidiService, private uiService: UiService) {}

  onRowClick(player: Player, event: MouseEvent) {
    let element = document.getElementById("player-row-" + player.id)
    if (this.selectedPlayer == undefined) {
      this.selectedPlayer = player
      this.playerSelectEvent.emit(player);
      this.uiService.swapClass(element, 'selected', 'active')
    }

    else if (this.selectedPlayer != player) {
      // this.onRowClick(this.selectedPlayer, event)
      let current = document.getElementById("player-row-" + this.selectedPlayer.id)
      this.uiService.swapClass(current, 'selected', 'active')
      this.selectedPlayer = player
      this.playerSelectEvent.emit(player);
      this.uiService.swapClass(element, 'selected', 'active')
    }

    else {
      this.selectedPlayer = undefined
      this.uiService.swapClass(element, 'selected', 'active')
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
            this.selectedPlayer = this.DUMMY_PLAYER
        });
        this.players = this.players.filter(p => p.id != player.id)
        break
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick(this.DUMMY_PLAYER, 'add')
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrument = instrument
    this.midiService.updatePlayer(player.id, this.INSTRUMENT, instrument.id).subscribe()
  }

  onNoteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, this.NOTE, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined && this.selectedPlayer == undefined)
      this.playerSelectEvent.emit(player);
  }
}
