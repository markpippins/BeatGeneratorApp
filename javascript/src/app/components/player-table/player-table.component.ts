import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { Player } from '../../models/player';
import { MidiService } from '../../services/midi.service';
import { Instrument } from 'src/app/models/instrument';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';
import { Listener } from 'src/app/models/listener';
import { MidiMessage } from 'src/app/models/midi-message';
import { PlayerUpdateType } from 'src/app/models/player-update-type';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css'],
})
export class PlayerTableComponent implements Listener, OnInit {
  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Output()
  ruleChangeEvent = new EventEmitter<Player>();

  selectedPlayers: Player[] = [];

  @Output()
  instruments!: Instrument[];

  @Input()
  players!: Player[];

  playerCols = Constants.PLAYER_COLUMNS;

  constructor(private midiService: MidiService, private uiService: UiService) {}

  onNotify(messageType: number, _message: string) {
    if (
      messageType == Constants.TICKER_SELECTED ||
      messageType == Constants.DISCONNECTED
    )
      this.selectedPlayers = [];
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
    });
  }

  onRowClick(player: Player) {
    let element = document.getElementById('player-row-' + player.id);
    if (this.selectedPlayers.length == 0) {
      this.selectedPlayers.push(player);
      this.playerSelectEvent.emit(player);
      this.uiService.swapClass(element, 'selected', 'active');
    } else if (this.selectedPlayers.length > 0) {
      this.selectedPlayers.forEach((p) => {
        let current = document.getElementById('player-row-' + p.id);
        this.uiService.swapClass(current, 'selected', 'active');
      });

      this.selectedPlayers = [player];
      this.playerSelectEvent.emit(player);
      this.uiService.swapClass(element, 'selected', 'active');
    }
    // else {
    //   this.selectedPlayer = undefined;
    //   this.uiService.swapClass(element, 'selected', 'active');
    // }
  }

  onBtnClick(player: Player, action: string) {
    switch (action) {
      case 'ticker-add': {
        this.midiService.addPlayer().subscribe(async (data) => {
          this.players.push(data);

          this.midiService
            .addRule(this.players[this.players.length - 1])
            .subscribe(async (data) => {
              this.players[this.players.length - 1].rules.push(data);
              this.ruleChangeEvent.emit(this.players[this.players.length - 1]);
            });
        });
        break;
      }
      case 'ticker-remove': {
        player.rules = [];
        this.midiService.removePlayer(player).subscribe(async (data) => {
          this.players = data;
          if (this.players.length == 0) this.selectedPlayers = [];
        });
        this.players = this.players.filter((p) => p.id != player.id);
        break;
      }
      case 'ticker-mute': {
        let index = this.players.indexOf(player);
        this.players[index].muted = !this.players[index].muted;
        this.midiService
          .updatePlayer(
            this.players[index].id,
            PlayerUpdateType.MUTE,
            this.players[index].muted ? 1 : 0
          )
          .subscribe((data) => (this.players[index] = data));
        break;
      }

      case 'ticker-audition': {
        if (this.selectedPlayers.length == 1) {
          // this.selectedPlayer.muted = !this.selectedPlayer.muted
          this.midiService.sendMessage(
            MidiMessage.NOTE_ON,
            this.selectedPlayers[0].channel,
            this.selectedPlayers[0].note,
            120
          );
          this.midiService.sendMessage(
            MidiMessage.NOTE_OFF,
            this.selectedPlayers[0].channel,
            this.selectedPlayers[0].note,
            120
          );
        }
        break;
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick(
      {
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
        skips: 0,
        beatFraction: 0,
        subDivisions: 0,
        playerClass: '',
        randomDegree: 0,
        fadeIn: 0,
        fadeOut: 0,
      },
      'add'
    );
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrumentId = instrument.id;
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.INSTRUMENT, instrument.id)
      .subscribe();
  }

  onNoteChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.NOTE, event.target.value)
      .subscribe();
  }

  onPartChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.PART, event.target.value)
      .subscribe();
  }

  onPresetChange(player: Player, event: { target: any }) {
    this.players
      .filter((p) => p.instrumentId == player.instrumentId)
      .forEach((p) =>
        this.midiService
          .updatePlayer(p.id, PlayerUpdateType.PRESET, event.target.value)
          .subscribe()
      );
    this.uiService.notifyAll(Constants.PLAYER_UPDATED, 'Player updated', 0);
  }

  onLevelChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.LEVEL, event.target.value)
      .subscribe();
  }

  onMinVelocityChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.MIN_VELOCITY,
        event.target.value
      )
      .subscribe();
  }

  onMaxVelocityChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.MAX_VELOCITY,
        event.target.value
      )
      .subscribe();
  }

  onRatchetCountChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RATCHET_COUNT,
        event.target.value
      )
      .subscribe();
  }

  onRatchetIntervalChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RATCHET_INTERVAL,
        event.target.value
      )
      .subscribe();
  }

  onFadeInChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.FADE_IN, event.target.value)
      .subscribe();
  }

  onFadeOutChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.FADE_OUT, event.target.value)
      .subscribe();
  }

  onProbabilityChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.PROBABILITY, event.target.value)
      .subscribe();
  }

  onSwingChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.SWING, event.target.value)
      .subscribe();
  }

  onBeatFractionChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.BEAT_FRACTION,
        event.target.value
      )
      .subscribe();
  }

  onRandomDegreeChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RANDOM_DEGREE,
        event.target.value
      )
      .subscribe();
  }

  onSkipsChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.SKIPS, event.target.value)
      .subscribe();
  }

  onSubsChange(player: Player, event: { target: any }) {
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.SUBDIVISIONS,
        event.target.value
      )
      .subscribe();
  }

  onPass(player: Player) {
    if (player != undefined && this.selectedPlayers.length == 0)
      this.playerSelectEvent.emit(player);
  }

  getMuteButtonClass(player: Player): string {
    return player.muted ? 'muted' : 'unmuted';
  }
}
