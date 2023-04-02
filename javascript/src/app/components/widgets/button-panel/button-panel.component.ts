import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-button-panel',
  templateUrl: './button-panel.component.html',
  styleUrls: ['./button-panel.component.css'],
})
export class ButtonPanelComponent implements Listener, OnInit {

  @Output()
  buttonClickedEvent = new EventEmitter<string>()

  @Input()
  exclusive = false;

  @Input()
  identifier = 'symbol';

  @Input()
  messageType = -1;

  @Input()
  window = 16;

  @Input()
  symbols: string[] = [];

  position = this.window;
  range: string[] = [];
  overage: string[] = [];
  selections: boolean[] = [];

  constructor(private uiService: UiService) {
    uiService.addListener(this);
  }
  ngOnInit(): void {
    this.updateDisplay();
  }

  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType = this.messageType)
      this.updateDisplay()
  }

  updateDisplay() {
    // this.midiService.symbolserInfo().subscribe(symbols => {
    this.position = this.window;
    this.symbols = [];
    this.selections = [];

    this.range = [];
    this.overage = [];

    for (let index = 0; index < this.symbols.length; index++) {
      // this.symbols.push(this.symbols[index]);
      this.selections.push(false);
      if (this.range.length < this.window) this.range.push(this.symbols[index]);
    }

    while (this.range.length + this.overage.length < this.window)
      this.overage.push('');
  }

  onForwardClicked() {
    if (this.range[this.window - 1] == this.symbols[this.symbols.length - 1])
      return;

    if (this.position >= this.symbols.length) return;

    this.range = [];
    this.overage = [];

    while (this.range.length < this.window) {
      if (this.position == this.symbols.length) break;
      else this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.window)
      this.overage.push('-');
  }

  onBackClicked() {
    if (this.position == 0) return;

    this.range = [];
    this.overage = [];
    this.position -= this.window * 2;
    if (this.position < 0) this.position = 0;

    while (
      this.position < this.symbols.length &&
      this.overage.length + this.range.length < this.window
    ) {
      while (this.position == this.symbols.length)
        this.overage.push('-');
      this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.window)
      this.overage.push('');
    // this.uiService. mini-symbols-btn-{{symbols}}
  }

  onClick(index: number, event: Event) {
    // let index = this.symbols.indexOf(symbol)
    this.selections[index] = !this.selections[index];
    this.buttonClickedEvent.emit(this.symbols[index])
    this.uiService.swapClass(
      event.target,
      'mini-nav-btn-selections',
      'mini-nav-btn'
    );
  }

  getButtonId(pos: number, over: boolean) : string {
    if (over)
      return this.identifier + 'mini-overage-btn-' + pos
    return this.identifier + 'mini-nav-btn-' + pos
  }
}
