import {Component, OnInit, Output} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Player} from "../../models/player";
import {MatTabsModule} from '@angular/material/tabs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private isPlaying: boolean = false;

  @Output()
  players!: Player[];

  @Output()
  selectedPlayer!: Player;
  constructor(private midiService: MidiService) {}

  ngOnInit(): void {
    this.updateDisplay();
    }
  onActionSelected(action: string) {
    switch (action) {
      case 'forward': {
        // this.toolBarTransportButtonClicked('stop');
        // this.delay(3000);
        // this.toolBarTransportButtonClicked('play');
        break;
      }
      case 'play': {
        this.midiService.startClicked().subscribe();
        this.isPlaying = true;
        // this.updateDisplay();
        break;
      }
      case 'stop': {
        this.midiService.stopClicked().subscribe();
        this.isPlaying = false;
        this.players = [];
        // this.playerConditions = [];
        break;
      }

      case 'pause': {
        this.midiService.pauseClicked().subscribe()
        // this.isPlaying = false
        // this.players = []
        // this.playerConditions = []
        break
      }

      case 'record': {
        this.midiService.recordClicked().subscribe();
        this.isPlaying = false;
        // this.players = [];
        // this.playerConditions = [];
        break;
      }

      case 'add': {
        this.midiService.addPlayerClicked().subscribe();
        break;
      }

      case 'clear': {
        this.midiService.clearPlayers().subscribe();
        break;
      }
    }
  }

  updateDisplay(): void {
    this.midiService.playerInfo().subscribe(async (data) => {
      // var update: boolean = this.isPlaying && this.players.length != (<Player[]>data).length
      this.players = data;
      // if (update && this.isPlaying) {
      await this.midiService.delay(2500);
      this.updateDisplay();
      // }
    });
  }

  onPlayerSelected(player: Player) {
    console.log(player.id)
    this.selectedPlayer = player
  }

  toggleClass(el: any, className: string) {
    if (el.className.indexOf(className) >= 0) {
      el.className = el.className.replace(className,"");
    }
    else {
      el.className  += className;
    }
  }

}
