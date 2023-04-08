import { Component, Input } from '@angular/core';
import { Ticker } from 'src/app/models/ticker';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';
import { MidiService } from 'src/app/services/midi.service';

@Component({
  selector: 'app-ticker-adjust',
  templateUrl: './ticker-adjust.component.html',
  styleUrls: ['./ticker-adjust.component.css']
})
export class TickerAdjustComponent {

  @Input()
  ticker!: Ticker

  constructor(private midiService: MidiService) {
    // uiService.addListener(this)
  }

  onClick(action: string) {
      if (action == 'up')
        this.midiService.updateTicker(this.ticker.id,  TickerUpdateType.BASE_NOTE_OFFSET, 1).subscribe()

      if (action == 'down')
        this.midiService.updateTicker(this.ticker.id,  TickerUpdateType.BASE_NOTE_OFFSET, -1).subscribe()
  }
}
