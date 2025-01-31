import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Instrument } from '../../../models/instrument';
import { Swirl } from 'src/app/models/swirl';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css'],
})
export class DrumPadComponent implements Listener {
  @Output()
  padPressedEvent = new EventEmitter<number>();

  @Input()
  name!: string;

  @Input()
  caption!: string;

  @Input()
  swirling = false;
  swirl = new Swirl<number>([0, 1, 2, 3, 4, 5, 6, 7]);

  @Input()
  selector: boolean = false;

  @Input()
  index!: number;

  @Input()
  note!: number;

  @Input()
  otherNote!: number;

  @Input()
  instrument!: Instrument;

  @Input()
  pressed!: boolean;

  @Input()
  active: boolean = false;

  @Input()
  muted: boolean = false;

  @Input()
  channel!: number;

  constructor(private uiService: UiService) {
    this.uiService.addListener(this);
  }

  pulse = 0;

  onNotify(_messageType: number, _message: string, _messageValue: number) {
    console.log("NOTIFIED")
    if (_messageType == Constants.TICKER_CONNECTED && this.swirling) {
      this.pulse++;
      this.swirl.forward();
    }
  }

  padPressed() {
    this.padPressedEvent.emit(this.index);
  }

  getPadClass(): string {
    let result = this.pressed
      ? 'pad pad-' + this.index + ' pressed'
      : 'pad pad-' + this.index;

    return result;
  }

  getPadRingClass(): string {
    if (this.selector) return 'none';

    let result = this.active ? 'pad-ring-active' : 'pad-ring';

    return result;
  }

  getIndicatorClass(index: number): string {
    if (this.swirling) return 'pad-' + (this.swirl.getItems()[index] + 3);

    let result = 'pad-indicator-content';

    switch (index) {
      case 0: {
        result += this.pressed ? ' armed' : ' mute';
        break;
      }
      case 1: {
        result += this.muted ? ' muted' : ' active';
        break;
      }
    }

    return result;
  }
}
