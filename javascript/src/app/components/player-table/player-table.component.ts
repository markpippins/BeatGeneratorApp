import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Player} from "../../models/player";
import {MidiService} from "../../services/midi.service";
import {Strike} from "../../models/strike";
import { Instrument } from 'src/app/models/instrument';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';
import { Listener } from 'src/app/models/listener';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css']
})
export class PlayerTableComponent implements Listener, OnInit {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  selectedPlayer: Player | undefined;


  @Input()
  players!: Player[]

  playerCols = Constants.PLAYER_COLUMNS

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  notify(messageType: number, message: string) {
    if (messageType == Constants.TICKER_SELECTED)
      this.selectedPlayer = undefined
  }

  ngOnInit(): void {
    this.uiService.addListener(this)
  }

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
            this.selectedPlayer = Constants.DUMMY_PLAYER
        });
        this.players = this.players.filter(p => p.id != player.id)
        break
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick(Constants.DUMMY_PLAYER, 'add')
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrument = instrument
    this.midiService.updatePlayer(player.id, Constants.INSTRUMENT, instrument.id).subscribe()
  }

  onNoteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.NOTE, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined && this.selectedPlayer == undefined)
      this.playerSelectEvent.emit(player);
  }
}
