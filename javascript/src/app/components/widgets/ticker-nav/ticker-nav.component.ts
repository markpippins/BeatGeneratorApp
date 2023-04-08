import { Component } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-ticker-nav',
  templateUrl: './ticker-nav.component.html',
  styleUrls: ['./ticker-nav.component.css']
})
export class TickerNavComponent {
  constructor(private uiService: UiService) {
    // uiService.addListener(this)
  }


  onClick(action: string) {
    this.uiService.notifyAll(Constants.COMMAND, action, 0)
  }
}
