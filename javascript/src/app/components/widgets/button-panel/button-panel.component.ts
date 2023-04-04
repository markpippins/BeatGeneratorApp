import {
  AfterViewChecked,
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core'
import { Listener } from 'src/app/models/listener'
import { UiService } from 'src/app/services/ui.service'

@Component({
  selector: 'app-button-panel',
  templateUrl: './button-panel.component.html',
  styleUrls: ['./button-panel.component.css'],
})
export class ButtonPanelComponent implements Listener, AfterViewChecked {
  @Output()
  buttonClickedEvent = new EventEmitter<string>()

  @Input()
  exclusive = true

  @Input()
  maxPressed = 0

  lastPressed!: String

  @Input()
  identifier = 'symbol'

  @Input()
  messageType = -1

  @Input()
  colCount = 16

  @Input()
  symbols!: string[]

  position = this.colCount
  range: string[] = []
  overage: string[] = []
  selections: boolean[] = []

  symbolCount = 0

  @Input()
  symbolBtnClassName = 'mini-nav-btn'

  getSymbolBtnSelectedClassName() : string { return this.symbolBtnClassName + '-selected'}

  @Input()
  navBtnClassName = 'mini-nav-btn'

  @Input()
  controlBtnClassName = 'mini-nav-btn'

  @Input()
  overageBtnClassName = 'mini-overflow-btn'

  @Input()
  containerClass = "flex-container-horizontal"

  constructor(private uiService: UiService) {
    uiService.addListener(this)
  }
  ngAfterViewChecked(): void {
    if (this.symbolCount != this.symbols.length) this.updateDisplay()

    this.symbolCount = this.symbols.length
  }

  onNotify(messageType: number, message: string, messageValue: number) {}

  updateDisplay() {
    this.position = this.colCount
    this.selections = []
    this.range = []

    this.symbols.forEach((s) => {
      this.selections.push(false)
      if (this.range.length < this.colCount) this.range.push(s)
    })

    this.overage = []
    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('')
  }

  onForwardClicked() {
    if (this.range[this.colCount - 1] == this.symbols[this.symbols.length - 1])
      return

    if (this.position >= this.symbols.length) return

    this.range = []
    this.overage = []

    // this.ticksPosition += this.colCount
    while (this.range.length < this.colCount) {
      if (this.position == this.symbols.length) break
      else this.range.push(this.symbols[this.position++])
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('')

    // this.updateSelections()
  }

  onBackClicked() {
    if (this.position == this.colCount) return

    this.range = []
    this.overage = []
    this.position -= this.colCount * 2
    if (this.position < 0) this.position = 0

    while (
      this.position < this.symbols.length &&
      this.overage.length + this.range.length < this.colCount
    ) {
      while (this.position == this.symbols.length) this.overage.push('-')
      this.range.push(this.symbols[this.position++])
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('')
    // this.uiService. mini-symbols-btn-{{symbols}}

    let index = 0
    this.selections.forEach((s) => {
      if (s)
        this.uiService.swapClass(
          this.getButtonId(index, false),
          this.getSymbolBtnSelectedClassName(),
          this.symbolBtnClassName
        )

      index++
    })
  }

  onClick(index: number, event: Event) {

    if (this.exclusive) {
      this.range.forEach(note => {
        let element = document.getElementById(this.symbolBtnClassName + index)
        if (element != undefined)
          this .uiService.removeClass(element, "-selected")
      })

      // this.selectedNote = note
      this.uiService.swapClass(event.target, this.symbolBtnClassName, this.getSymbolBtnSelectedClassName())
    }
    else {
      this.selections[index] = !this.selections[index]
      this.buttonClickedEvent.emit(this.symbols[index])
      this.uiService.swapClass(
        event.target,
        'mini-nav-btn-selected',
        'mini-nav-btn'
      )
    }
  }

  getButtonId(pos: number, over: boolean): string {
    if (over) return this.identifier + 'mini-overage-btn-' + pos
    return this.identifier + 'mini-nav-btn-' + pos
  }

  // updateSelections() {
  //   let index = 0
  //   this.selectedTicks.forEach(t => {
  //     let name = "mini-tick-btn-" + index
  //     let element = document.getElementById(name)
  //     if (element != undefined)
  //       this.uiService.addClass(element, "mini-nav-btn-selected")
  //   })
  // }
}
