import {Component, EventEmitter, Input, Output} from '@angular/core'
import {MidiService} from "../../services/midi.service"
import {Player} from "../../models/player"
import {Rule} from "../../models/rule"
import { UiService } from 'src/app/services/ui.service'
import { Constants } from 'src/app/models/constants'

@Component({
  selector: 'app-rule-table',
  templateUrl: './rule-table.component.html',
  styleUrls: ['./rule-table.component.css']
})
export class RuleTableComponent {

  @Output()
  ruleChangeEvent = new EventEmitter<Player>()
  EQUALS = 0
  GREATER_THAN = 1
  LESS_THAN = 2
  MODULO = 3
  COMPARISON = ["=", ">", "<", "%", "*", "/"]
  TICK = 0
  BEAT = 1
  BAR = 2
  PART = 3
  POSITION = 4
  OPERATOR = ["TICK", "BEAT", "BAR", "PART", "POSITION"]

  @Input()
  player!: Player
  ruleCols: string[] = [
    // 'ID',
    'Operator',
    'Op',
    'Value',
    'Part'
  ]

  constructor(private midiService: MidiService, private uiService: UiService) {

  }

  ngAfterContentChecked(): void {
    this.getRules().forEach(rule => {
      let op = 'operatorSelect-' + rule.id
      this.uiService.setSelectValue(op, rule.operatorId)
    })
    this.getRules().forEach(rule => {
      let co = 'comparisonSelect-' + rule.id
      this.uiService.setSelectValue(co, rule.comparisonId)
    })
  }

  getRules(): Rule[] {
    return this.player == undefined ? [] : this.player.rules
  }

  onOperatorChange(rule: Rule, event: { target: any }) {
    let value = this.OPERATOR.indexOf(event.target.value)
    this.midiService.updateRule(this.player.id, rule.id, value, rule.comparisonId, rule.value, rule.part).subscribe()
    rule.operatorId = value
    // let op = 'operatorSelect-' + rule.id
    this.uiService.setSelectValue(event.target, value)
    this.uiService.notifyAll(Constants.STATUS, this.OPERATOR[value] + ' selected.')
  }

  onComparisonChange(rule: Rule, event: { target: any }) {
    let value = this.COMPARISON.indexOf(event.target.value)
    this.midiService.updateRule(this.player.id, rule.id, rule.operatorId, value, rule.value, rule.part).subscribe()
    rule.comparisonId = value
    this.uiService.setSelectValue(event.target, value)
    this.uiService.notifyAll(Constants.STATUS, this.COMPARISON[value] + ' selected.')
  }

  onValueChange(rule: Rule, event: { target: any }) {
    rule.value = event.target.value
    this.midiService.updateRule(this.player.id, rule.id, rule.operatorId, rule.comparisonId, event.target.value, rule.part).subscribe()
  }

  onPartChange(rule: Rule, event: { target: any }) {
    rule.part = event.target.value
    this.midiService.updateRule(this.player.id, rule.id, rule.operatorId, rule.comparisonId, rule.value, rule.part).subscribe()
  }


  btnClicked(rule: Rule, command: string) {
    if (this.player.id > 0)
      switch (command) {
        case 'add': {
          this.midiService.addRule(this.player).subscribe(async (data) => {
            this.player.rules.push(data)
            this.ruleChangeEvent.emit(this.player)
          })
          break
        }
        case 'remove': {
          this.player.rules = this.player.rules.filter(r => r.id != rule.id)
          this.midiService.removeRule(this.player, rule).subscribe()
          this.ruleChangeEvent.emit(this.player)
          break
        }
      }
  }

  initBtnClick() {
    if (this.player.id > 0)
      this.midiService.addRule(this.player).subscribe(async (data) => {
        this.player.rules.push(data)
      })
  }
}
