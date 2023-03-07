import {Component, OnInit, Output} from '@angular/core'
import {MidiService} from "../../services/midi.service"
import {Player} from "../../models/player"
import {MatTabsModule} from '@angular/material/tabs'
import {Ticker} from "../../models/ticker"

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
  ticker!: Ticker

  running = false

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.updateDisplay()
    this.onActionSelected('forward')
  }

  onActionSelected(action: string) {
    switch (action) {
      case 'forward': {
        this.midiService.next(this.ticker == undefined ? 0 : this.ticker.id).subscribe(async (data) => {
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
        this.midiService.stop().subscribe()
        this.players = []

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

  private clear() {
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
  }
}
