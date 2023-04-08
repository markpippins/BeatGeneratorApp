import { Component, OnInit, Output } from '@angular/core'
import { MidiService } from "../../services/midi.service"
import { Player } from "../../models/player"
import { Ticker } from "../../models/ticker"
import { UiService } from 'src/app/services/ui.service'
import { Constants } from 'src/app/models/constants'
import { Listener } from 'src/app/models/listener'

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, Listener {

  @Output()
  players!: Player[]

  @Output()
  selectedPlayer: Player | undefined

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
    partLength: 4,
    playing: false,
    songLength: 0,
    stopped: false,
    swing: 0,
    tempoInBPM: 0,
    tick: 0,
    ticksPerBeat: 0,
    activePlayerIds: [],
    part: 0,
    bars: 16,
    parts: 4,
    beats: 0,
    barCount: 0,
    beatCount: 0,
    tickCount: 0,
    partCount: 0,
    baseNoteOffSet: 0,
    players: []
  }

  running = false

  @Output()
  consoleOutput: string[] = []

  constructor(private midiService: MidiService, private uiService: UiService) {
    uiService.addListener(this)
  }

  ngOnInit(): void {
    this.updateDisplay()
    this.onActionSelected('forward')
  }

  onNotify(_messageType: number, _message: string) {
    this.consoleOutput.pop()
    switch (_messageType) {
      case Constants.STATUS:
        this.consoleOutput.push(_message)
        break

      case Constants.COMMAND:
        this.onActionSelected(_message)
        break

      case Constants.CONNECTED:
        this.consoleOutput.push('connected')
        this.updateDisplay()
        break

      case Constants.DISCONNECTED:
        this.consoleOutput.push('disconnected')
        break

    case Constants.PLAYER_UPDATED:
      this.consoleOutput.push(_message)
      this.updateDisplay()
      break

    }
  }

  onActionSelected(action: string) {
    this.consoleOutput.pop()
    this.consoleOutput.push(action)

    if (this.ticker != undefined)
      switch (action) {
        case 'ticker-forward': {
          if (this.ticker.id > 0 && this.ticker.playing) {
            this.consoleOutput.pop()
            this.consoleOutput.push('ticker is currently playing')
          } else this.midiService.next(this.ticker.id).subscribe(async (data) => {
            this.clear();
            this.ticker = data
            this.midiService.playerInfo().subscribe(data => {
              this.players = data
              if (this.players.length > 0)
                this.selectedPlayer = this.players[0]
            })
          })
          this.uiService.notifyAll(Constants.TICKER_SELECTED, this.ticker.id.toString(), 0)
          break
        }

        case 'ticker-previous': {
          if (this.ticker != undefined && this.ticker.id > 0) {
            this.midiService.previous(this.ticker.id).subscribe(async (data) => {
              this.clear();
              this.ticker = data
              this.midiService.playerInfo().subscribe(data => {
                this.players = data
                this.sortByPitch(this.players)
                if (this.players.length > 0)
                  this.selectedPlayer = this.players[0]
              })
            })
          }
          this.uiService.notifyAll(Constants.TICKER_SELECTED, this.ticker.id.toString(), 0)
          break
        }

        case 'ticker-play': {
          this.midiService.start().subscribe()
          this.updateDisplay()
          // let element = document.getElementById('transport-btn-play')
          // if (element != null) { // @ts-ignore
          //   this.toggleClass(element, 'active')
          // }

          break
        }

        case 'ticker-stop': {
          this.midiService.stop().subscribe(data => {
            this.ticker = data
          })
          this.players = []
          this.updateDisplay();
          // let element = document.getElementById('transport-btn-play')
          // if (element != null) { // @ts-ignore
          //   this.toggleClass(element, 'active')
          // }
          break
        }

        case 'ticker-pause': {
          this.midiService.pause().subscribe()
          // this.isPlaying = false
          // this.players = []
          // this.playerConditions = []
          break
        }

        case 'ticker-record': {
          this.midiService.record().subscribe()
          // this.players = []
          // this.playerConditions = []
          break
        }

        case 'ticker-add': {
          this.midiService.addPlayer().subscribe(async (data) => {
            this.players.push(data)
            this.selectedPlayer = data
          })
          break
        }

        case 'ticker-remove': {
          if (this.selectedPlayer == undefined)
            break
          let id = this.selectedPlayer.id
          this.selectedPlayer.rules = []
          this.midiService.removePlayer(this.selectedPlayer).subscribe(async (data) => {
            this.players = data;
            if (this.players.length == 0)
              this.selectedPlayer = undefined
          });
          this.players = this.players.filter(p => p.id != id)

          if (this.players.length > 0)
            this.selectedPlayer = this.players[this.players.length - 1]
          break
        }


        case 'ticker-refresh': {
          this.updateDisplay();
          break
        }

        case 'save': {
          this.midiService.saveConfig().subscribe();
          break
        }

        case 'ticker-clear': {
          this.midiService.clearPlayers().subscribe()
          this.clear()
          break
        }
      }

    this.updateDisplay()
  }

  updateDisplay(): void {
    this.midiService.playerInfo().subscribe(async (data) => {
      var update: boolean = this.ticker.playing && this.players.length != (<Player[]>data).length
      this.players = data // this.sortByPitch(data)
      this.players.forEach(p => p.active = p.id in this.ticker.activePlayerIds)
      if (update && this.ticker.playing) {
        await this.midiService.delay(1000)
        this.updateDisplay()
      }
    })
  }

  onPlayerSelected(player: Player) {
    this.selectedPlayer = player
    this.uiService.notifyAll(Constants.PLAYER_SELECTED, "Player selected", player.id)
  }

  refresh() {
    this.updateDisplay()
  }

  clear() {
    this.selectedPlayer = undefined
  }

 sortByPitch(data: Player[]): any[] {
    return data.sort((a, b) => {
      if (a.note > b.note) {
        return 1;
      }
      if (a.note < b.note) {
        return -1;
      }
      return 0;
    });
  }
}
