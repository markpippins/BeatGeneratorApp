import { Component, Input } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Player } from 'src/app/models/player';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.css']
})
export class SchedulerComponent {
  @Input()
  players!: Player[];

  @Input()
  selectedPlayer: Player | undefined;

  constructor(private uiService: UiService) {

  }

  onPlayerSelected(player: Player) {
    this.selectedPlayer = player;
    this.uiService.notifyAll(
      Constants.PLAYER_SELECTED,
      'Player selected',
      player.id
    );
  }

}
