import { Component } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-player-nav',
  templateUrl: './player-nav.component.html',
  styleUrls: ['./player-nav.component.css']
})
export class PlayerNavComponent {
    constructor(private uiService: UiService) {
      // uiService.addListener(this)
    }


    onClick(action: string) {
      this.uiService.notifyAll(Constants.COMMAND, action, 0)
    }
}
