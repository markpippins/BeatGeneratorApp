import {
  AfterContentChecked,
  AfterViewChecked,
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-button-panel',
  templateUrl: './button-panel.component.html',
  styleUrls: ['./button-panel.component.css'],
})
export class ButtonPanelComponent
  implements Listener, AfterViewChecked, AfterContentChecked
{
  @Output()
  buttonClickedEvent = new EventEmitter<string>();

  @Input()
  exclusive = false;

  @Input()
  maxPressed = 0;

  lastPressed!: String;

  @Input()
  identifier = 'symbol';

  @Input()
  messageType = -1;

  @Input()
  colCount = 16;

  @Input()
  symbols!: string[];

  position = this.colCount;
  range: string[] = [];
  overage: string[] = [];
  selections: boolean[] = [];

  symbolCount = 0;

  @Input()
  symbolBtnClassName = 'mini-nav-btn';

  getSymbolBtnSelectedClassName(): string {
    return this.symbolBtnClassName + '-selected';
  }

  @Input()
  controlBtnClassName = 'mini-nav-btn, mini-control-btn';

  @Input()
  overageBtnClassName = 'mini-overflow-btn';

  @Input()
  containerClass = 'flex-container-horizontal';

  constructor(private uiService: UiService) {
    uiService.addListener(this);
  }

  ngAfterContentChecked(): void {
    if (this.symbolCount != this.symbols.length) {
      this.updateDisplay();
      this.symbolCount = this.symbols.length;
    }
  }

  ngAfterViewChecked(): void {
    this.updateSelections()
  }

  onNotify(messageType: number, message: string, messageValue: number) {}

  updateDisplay() {
    this.position = this.colCount;
    this.selections = [];
    this.range = [];

    this.symbols.forEach((s) => {
      this.selections.push(false);
      if (this.range.length < this.colCount) this.range.push(s);
    });

    this.overage = [];
    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');
  }

  onForwardClicked() {
    if (this.range[this.colCount - 1] == this.symbols[this.symbols.length - 1])
      return;

    if (this.position >= this.symbols.length) return;

    this.range = [];
    this.overage = [];

    // this.ticksPosition += this.colCount
    while (this.range.length < this.colCount) {
      if (this.position == this.symbols.length) break;
      else this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');

    this.updateSelections();
  }

  onBackClicked() {
    if (this.position == this.colCount) return;

    this.range = [];
    this.overage = [];
    this.position -= this.colCount * 2;
    if (this.position < 0) this.position = 0;

    while (
      this.position < this.symbols.length &&
      this.overage.length + this.range.length < this.colCount
    ) {
      while (this.position == this.symbols.length) this.overage.push('');
      this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');
    this.updateSelections();
  }

  onClick(index: number, event: Event) {
    if (this.exclusive) {
      this.range.forEach((note) => {
        let element = document.getElementById(this.symbolBtnClassName + index);
        if (element != undefined) {
          this.uiService.removeClass(
            element,
            this.getSymbolBtnSelectedClassName()
          );
          this.uiService.addClass(element, this.symbolBtnClassName);
        }
      });

      // this.selectedNote = note
      this.uiService.swapClass(
        event.target,
        this.symbolBtnClassName,
        this.getSymbolBtnSelectedClassName()
      );
    } else {
      this.selections[index] = !this.selections[index];
      this.uiService.swapClass(
        event.target,
        this.getSymbolBtnSelectedClassName(),
        this.symbolBtnClassName
      );
    }

    this.buttonClickedEvent.emit(this.symbols[index]);
  }

  getButtonId(pos: number, over: boolean): string {
    if (over) this.identifier + '-' + this.overageBtnClassName + '-' + String(this.position + pos);
    return this.identifier + '-' + this.symbolBtnClassName + '-' + String(this.position + pos);
  }

  updateSelections() {
    let index = this.position - this.colCount;
    console.log('selected buttons:');
    this.selections.forEach((t) => {
      let id = this.getButtonId(index, false);
      let element = document.getElementById(id);
      if (element != undefined)
        if (this.selections[index]) {
          this.uiService.swapClass(
            element,
            this.getSymbolBtnSelectedClassName(),
            this.symbolBtnClassName
          );
        }
      index++;
    });
  }
}
