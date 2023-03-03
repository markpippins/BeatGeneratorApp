import {Component, OnInit, Output} from '@angular/core'
import {MidiService} from "../../services/midi.service"
import {Player} from "../../models/player"
import {MatTabsModule} from '@angular/material/tabs'
import {Ticker} from "../../models/ticker";

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

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.updateDisplay()
  }

  onActionSelected(action: string) {
    switch (action) {
      case 'forward': {
        this.midiService.nextClicked(this.ticker == undefined ? 0 : this.ticker.id).subscribe(async (data) => {
          this.ticker = data;
          this.players = this.ticker.players
        })
        break
      }

      case 'previous': {

        this.midiService.previousClicked(this.ticker == undefined ? 0 : this.ticker.id).subscribe(async (data) => {
          this.ticker = data;
          this.players = this.ticker.players
        })
        break
      }

      case 'play': {
        this.midiService.startClicked().subscribe()
        // this.updateDisplay()
        let element = document.getElementById('transport-btn-play')
        if (element != null) { // @ts-ignore
          this.toggleClass(element, 'active')
        }

        break
      }

      case 'stop': {
        this.midiService.stopClicked().subscribe()
        this.players = []

        let element = document.getElementById('transport-btn-play')
        if (element != null) { // @ts-ignore
          this.toggleClass(element, 'active')
        }
        break
      }

      case 'pause': {
        this.midiService.pauseClicked().subscribe()
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'record': {
        this.midiService.recordClicked().subscribe()
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'add': {
        this.midiService.addPlayerClicked().subscribe(async (data) => {
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
        this.selectedPlayer = {
          allowedControlMessages: [],
          channel: 0,
          id: 0,
          instrument: "",
          maxVelocity: 0,
          minVelocity: 0,
          note: 0,
          preset: 0,
          probability: 0,
          rules: []
        }
        break
      }
    }

    this.updateDisplay()
  }

  updateDisplay(): void {
    this.midiService.playerInfo().subscribe(async (data) => {
      // var update: boolean = this.isPlaying && this.players.length != (<Player[]>data).length
      this.players = data
      // if (update && this.isPlaying) {
      // await this.midiService.delay(1000)
      // this.updateDisplay()
      // }
    })
  }

  onPlayerSelected(player: Player) {
    console.log(player.id)
    this.selectedPlayer = player
  }

  toggleClass(el: any, className: string) {
    if (el.className.indexOf(className) >= 0) {
      el.className = el.className.replace(className, "")
    } else {
      el.className += className
    }
  }

  refresh() {
    this.updateDisplay()
  }
}
