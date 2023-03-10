import {Component, OnInit, Output} from '@angular/core'
import {MidiService} from "../../services/midi.service"
import {Player} from "../../models/player"
import {MatTabsModule} from '@angular/material/tabs'
import {Ticker} from "../../models/ticker"
import { UiService } from 'src/app/services/ui.service'

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  @Output()
  players!: Player[]

  @Output()
  selectedPlayer!: Player

  tickerPointer = 0
  @Output()
  ticker: Ticker = {
    bar: 0,
    beat: 0,
    beatDivider: 0,
    beatsPerBar: 0,
    done: false,
    id: 0,
    maxTracks: 0,
    partLength: 0,
    players: [],
    playing: false,
    songLength: 0,
    stopped: false,
    swing: 0,
    tempoInBPM: 0,
    tick: 0,
    ticksPerBeat: 0
  }

  running = false

  @Output()
  consoleOutput: string[] = []

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  ngOnInit(): void {
    this.updateDisplay()
    this.onActionSelected('forward')
  }

  onActionSelected(action: string) {
    this.consoleOutput.pop()
    this.consoleOutput.push(action)

    switch (action) {
      case 'forward': {
        if (this.ticker.id > 0 && this.ticker.playing) {
          this.consoleOutput.pop()
          this.consoleOutput.push('ticker is currently playing')
        } else this.midiService.next(this.ticker == undefined ? 0 : this.ticker.id).subscribe(async (data) => {
          this.clear();
          this.ticker = data
          this.players = this.ticker.players
          if (this.players.length > 0)
            this.selectedPlayer = this.players[0]
        })
        break
      }

      case 'previous': {
        if (this.ticker != undefined && this.ticker.id > 0) {
          this.midiService.previous(this.ticker.id).subscribe(async (data) => {
            this.clear();
            this.ticker = data
            this.players = this.ticker.players
            if (this.players.length > 0)
              this.selectedPlayer = this.players[0]
          })
        }
        break
      }

      case 'play': {
        this.midiService.start().subscribe()
        this.updateDisplay()
        // let element = document.getElementById('transport-btn-play')
        // if (element != null) { // @ts-ignore
        //   this.toggleClass(element, 'active')
        // }

        break
      }

      case 'stop': {
        this.midiService.stop().subscribe(data => {
          this.ticker = data
        })
        // this.players = []
        // let element = document.getElementById('transport-btn-play')
        // if (element != null) { // @ts-ignore
        //   this.toggleClass(element, 'active')
        // }
        break
      }

      case 'pause': {
        this.midiService.pause().subscribe()
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'record': {
        this.midiService.record().subscribe()
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'add': {
        this.midiService.addPlayer().subscribe(async (data) => {
          this.players.push(data)
          this.selectedPlayer = data
        })
        break
      }

      case 'refresh': {
        // this.midiService.refreshClicked().subscribe(async (data) => {
        // this.players.push(data)
        // this.selectedPlayer = data
        // })
        break
      }

      case 'save': {
        alert('save')
        break
      }

      case 'clear': {
        this.midiService.clearPlayers().subscribe()
        this.clear()
        break
      }
    }

    this.updateDisplay()
  }

  updateDisplay(): void {
    this.midiService.playerInfo().subscribe(async (data) => {
      // var update: boolean = this.isPlaying && this.players.length != (<Player[]>data).length
      // this.players = data
      // if (update && this.isPlaying) {
      // await this.midiService.delay(1000)
      // this.updateDisplay()
      // }
    })
  }

  onPlayerSelected(player: Player) {
    this.selectedPlayer = player
  }

  refresh() {
    this.updateDisplay()
  }

  private clear() {
    this.selectedPlayer = this.DUMMY_PLAYER
  }

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

}
