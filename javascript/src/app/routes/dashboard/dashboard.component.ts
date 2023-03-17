import {Component, OnInit, Output} from '@angular/core'
import {MidiService} from "../../services/midi.service"
import {Player} from "../../models/player"
import {MatTabsModule} from '@angular/material/tabs'
import {Ticker} from "../../models/ticker"
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
    partLength: 0,
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
    uiService.addListener(this)
  }


  ngOnInit(): void {
    this.updateDisplay()
    this.onActionSelected('forward')
  }

  onNotify(messageType: number, message: string) {
    this.consoleOutput.pop()
    switch(messageType) {
      case Constants.STATUS:
        this.consoleOutput.push(message)
        break

      case Constants.CONNECTED:
        this.consoleOutput.push('connected')
        break

      case Constants.DISCONNECTED:
        this.consoleOutput.push('disconnected')
        break

      }
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
          this.midiService.playerInfo().subscribe(data => {
            this.players = data
            if (this.players.length > 0)
              this.selectedPlayer = this.players[0]
          })
        })
        this.uiService.notifyAll(Constants.TICKER_SELECTED, this.ticker.id.toString())
        break
      }

      case 'previous': {
        if (this.ticker != undefined && this.ticker.id > 0) {
          this.midiService.previous(this.ticker.id).subscribe(async (data) => {
            this.clear();
            this.ticker = data
            this.midiService.playerInfo().subscribe(data => {
              this.players = data
              if (this.players.length > 0)
                this.selectedPlayer = this.players[0]
            })
          })
        }
        this.uiService.notifyAll(Constants.TICKER_SELECTED, this.ticker.id.toString())
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
        this.uiService.notifyAll(Constants.TICKER_SELECTED, this.ticker.id.toString())
        break
      }

      case 'save': {
        this.midiService.saveConfig().subscribe();
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
    this.selectedPlayer = undefined
  }

}
