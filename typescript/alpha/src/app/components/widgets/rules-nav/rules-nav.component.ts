import { Component } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-rules-nav',
  templateUrl: './rules-nav.component.html',
  styleUrls: ['./rules-nav.component.css']
})
export class RulesNavComponent {
  constructor(private uiService: UiService) {
    // uiService.addListener(this)
  }

  onClick(action: string) {
    this.uiService.notifyAll(Constants.COMMAND, action, 0)
  }
}
