import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Player} from "../../models/player";
import {MidiService} from "../../services/midi.service";
import {Strike} from "../../models/strike";
import { Instrument } from 'src/app/models/instrument';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';
import { Listener } from 'src/app/models/listener';
import { MidiMessage } from 'src/app/models/midi-message';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css']
})
export class PlayerTableComponent implements Listener, OnInit {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  selectedPlayer: Player | undefined;

  @Output()
  instruments!: Instrument[]

  @Input()
  players!: Player[]

  playerCols = Constants.PLAYER_COLUMNS

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  onNotify(messageType: number, message: string) {
    if (messageType == Constants.TICKER_SELECTED || messageType == Constants.DISCONNECTED)
      this.selectedPlayer = undefined
  }

  ngOnInit(): void {
    this.uiService.addListener(this)
    this.midiService.allInstruments().subscribe(data => {
      this.instruments = data
    })
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
            this.selectedPlayer = undefined
        });
        this.players = this.players.filter(p => p.id != player.id)
        break
      }
      case 'mute': {
        if (this.selectedPlayer != undefined) {
          this.selectedPlayer.muted = !this.selectedPlayer.muted
          this.midiService.updatePlayer(this.selectedPlayer?.id, Constants.MUTE, this.selectedPlayer.muted ? 1 : 0).subscribe()
        }
        break
      }

      case 'audition': {
        if (this.selectedPlayer != undefined) {
          // this.selectedPlayer.muted = !this.selectedPlayer.muted
          this.midiService.sendMessage(MidiMessage.NOTE_ON, this.selectedPlayer.channel, this.selectedPlayer.note, 120)
          this.midiService.sendMessage(MidiMessage.NOTE_OFF, this.selectedPlayer.channel, this.selectedPlayer.note, 120)
        }
        break
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick({
      id: 0,
      tickerId: 0,
      instrumentId: 0,
      maxVelocity: 0,
      minVelocity: 0,
      note: 0,
      preset: 0,
      probability: 0,
      rules: [],
      allowedControlMessages: [],
      parts: [],
      muted: false,
      name: '',
      channel: 0,
      swing: 0,
      level: 0,
      active: false,
      ratchetCount: 0,
      ratchetInterval: 0,
      subs: 0,
      sub: 0
    }, 'add')
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrumentId = instrument.id
    this.midiService.updatePlayer(player.id, Constants.INSTRUMENT, instrument.id).subscribe()
  }

  onNoteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.NOTE, event.target.value).subscribe()
  }

  onPartChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.PART, event.target.value).subscribe()
  }

  onPresetChange(player: Player, event: { target: any; }) {
    this.players.filter(p => p.instrumentId == player.instrumentId )
      .forEach(p => this.midiService.updatePlayer(p.id, Constants.PRESET, event.target.value).subscribe())
  }

  onLevelChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.LEVEL, event.target.value).subscribe()
  }

  onMinVelocityChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.MIN_VELOCITY, event.target.value).subscribe()
  }

  onMaxVelocityChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.MAX_VELOCITY, event.target.value).subscribe()
  }

  onRatchetCountChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.RATCHET_COUNT, event.target.value).subscribe()
  }

  onRatchetIntervalChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.RATCHET_INTERVAL, event.target.value).subscribe()
  }

  onProbabilityChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.PROBABILITY, event.target.value).subscribe()
  }

  onSwingChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, Constants.SWING, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined && this.selectedPlayer == undefined)
      this.playerSelectEvent.emit(player);
  }
}
