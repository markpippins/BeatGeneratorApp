import { AfterViewChecked, AfterViewInit, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-button-panel',
  templateUrl: './button-panel.component.html',
  styleUrls: ['./button-panel.component.css'],
})
export class ButtonPanelComponent implements Listener, OnInit, AfterViewInit, AfterViewChecked {

  @Output()
  buttonClickedEvent = new EventEmitter<string>()

  @Input()
  exclusive = false;

  @Input()
  identifier = 'symbol';

  @Input()
  messageType = -1;

  @Input()
  colCount = 16;

  @Input()
  symbols!: string[]

  position = this.colCount;
  range: string[] = [];
  overage: string[] = [];
  selections: boolean[] = [];

  symbolCount = 0

  constructor(private uiService: UiService) {
    uiService.addListener(this);
  }
  ngAfterViewChecked(): void {
    if (this.symbolCount != this.symbols.length)
      this.updateDisplay();

    this.symbolCount = this.symbols.length
  }
  ngAfterViewInit(): void {
  }

  ngOnInit(): void {
  }

  onNotify(messageType: number, message: string, messageValue: number) {
  }

  updateDisplay() {
    this.position = this.colCount;
    this.selections = [];
    this.range = [];

    this.symbols.forEach(s => {
      if (this.selections.length < this.symbols.length)
        this.selections.push(false);
        if (this.range.length < this.colCount)
        this.range.push(s);
    })

    this.overage = [];
    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');
  }

  onForwardClicked() {
    if (this.range[this.colCount - 1] == this.symbols[this.symbols.length - 1])
      return

    if (this.position >= this.symbols.length)
      return

    this.range = []
    this.overage = []

    // this.ticksPosition += this.colCount
    while (this.range.length < this.colCount) {
      if (this.position == this.symbols.length)
        break
      else this.range.push(this.symbols[this.position++])
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('')

    // this.updateSelections()
  }

  onBackClicked() {
    if (this.position == 0) return;

    this.range = [];
    this.overage = [];
    this.position -= this.colCount * 2;
    if (this.position < 0) this.position = 0;

    while (
      this.position < this.symbols.length &&
      this.overage.length + this.range.length < this.colCount
    ) {
      while (this.position == this.symbols.length)
        this.overage.push('-');
      this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.colCount)
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
