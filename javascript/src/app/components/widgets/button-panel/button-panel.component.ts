import {
  AfterContentChecked,
  AfterContentInit,
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
  implements Listener, AfterContentInit, AfterContentChecked, AfterViewInit, AfterViewChecked
{
  @Output()
  buttonClickedForIndexEvent = new EventEmitter<number>();

  @Output()
  buttonClickedForCommandEvent = new EventEmitter<string>();

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

  @Input()
  customControls: string[] = ['-', '+'];
  visibleCustomControls: string[] = [];

  @Input()
  customControlMinCount: number = 0;

  position = this.colCount;
  range: string[] = [];
  overage: string[] = [];
  selections: boolean[] = [];

  symbolCount = 0;

  @Input()
  symbolBtnClassName = 'mini-btn';

  getSymbolBtnSelectedClassName(): string {
    return this.symbolBtnClassName + '-selected';
  }

  @Input()
  controlBtnClassName = 'mini-btn, mini-control-btn';

  @Input()
  overageBtnClassName = 'overflow';

  @Input()
  containerClass = 'flex-container-horizontal';

  constructor(private uiService: UiService) {
    uiService.addListener(this);
  }
  ngAfterContentInit(): void {
    console.log('ngAfterContentInit')
  }

  ngAfterViewInit(): void {
    console.log('ngAfterViewInit')
  }

  ngAfterContentChecked(): void {
    console.log('ngAfterContentChecked')
    if (this.symbolCount != this.symbols.length) {
      this.updateDisplay();
      this.symbolCount = this.symbols.length;
    }
    // this.updateSelections();
  }

  ngAfterViewChecked(): void {
    console.log('ngAfterViewChecked')
    this.updateSelections();
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

    this.visibleCustomControls = []
    let index = 0
    while (this.range.length + this.overage.length < this.colCount && index < this.customControls.length)
      this.visibleCustomControls.push(this.customControls[index++])

    this.overage = [];
    while (this.range.length + this.visibleCustomControls.length + this.overage.length < this.colCount)
      this.overage.push('');
  }

  onForwardClicked() {
    if (this.range[this.colCount - 1] == this.symbols[this.symbols.length - 1])
      return;

    if (this.position >= this.symbols.length) return;

    this.range = [];
    this.overage = [];

    // this.ticksPosition += this.colCount
    while (this.range.length < this.colCount + this.customControlMinCount) {
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
    if (this.position < 0)
      this.position = 0;

    while ( this.position < this.symbols.length && this.overage.length + this.range.length < this.colCount ) {
      while (this.position == this.symbols.length)
        this.overage.push('');
      this.range.push(this.symbols[this.position++]);
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');
    this.updateSelections();
  }

  onClick(index: number, event: Event) {
    // if (this.exclusive) {
    //   this.range.forEach((note) => {
    //     let element = document.getElementById(this.symbolBtnClassName + index);
    //     if (element != undefined) {
    //       this.uiService.removeClass(
    //         element,
    //         this.getSymbolBtnSelectedClassName()
    //       );
    //       this.uiService.addClass(element, this.symbolBtnClassName);
    //     }
    //   });

    //   this.uiService.swapClass(
    //     event.target,
    //     this.symbolBtnClassName,
    //     this.getSymbolBtnSelectedClassName()
    //   );
    // } else {
    this.selections[index] = !this.selections[index];
    this.uiService.swapClass(
      event.target,
      this.getSymbolBtnSelectedClassName(),
      this.symbolBtnClassName
    );
    // }

    this.buttonClickedForIndexEvent.emit(index);
    this.buttonClickedForCommandEvent.emit(this.symbols[index]);
  }

  getButtonId(pos: number, over: boolean): string {
    if (over)
      this.identifier +
        '-' +
        this.overageBtnClassName +
        '-' +
        String(this.position + pos);
    return (
      this.identifier +
      '-' +
      this.symbolBtnClassName +
      '-' +
      String(this.position + pos)
    );
  }

  updateSelections() {
    let index = this.position - this.colCount;
    this.selections.forEach((t) => {
      let id = this.getButtonId(index, false);
      let element = document.getElementById(id);
      if (element != undefined)
        if (this.selections[index]) {
          if (!this.uiService.hasClass(element, this.getSymbolBtnSelectedClassName()))
          this.uiService.swapClass(
            element,
            this.symbolBtnClassName,
            this.getSymbolBtnSelectedClassName(),
          );
        }
      index++;
    });
  }
}
