import {Component, Input} from '@angular/core';
import {MidiService} from "../../services/midi.service";
import {Player} from "../../models/player";
import {Rule} from "../../models/rule";

@Component({
  selector: 'app-rule-table',
  templateUrl: './rule-table.component.html',
  styleUrls: ['./rule-table.component.css']
})
export class RuleTableComponent {

  EQUALS = 0
  GREATER_THAN = 1
  LESS_THAN = 2
  MODULO = 3
  COMPARISON = ["EQUALS", "GREATER_THAN", "LESS_THAN", "MODULO"]
  TICK = 0
  BEAT = 1
  BAR = 2
  PART = 3
  POSITION = 4
  OPERATOR = ["TICK", "BEAT", "BAR", "PART", "POSITION"]

  @Input()
  player!: Player
  ruleCols: string[] = [
    'Operator',
    'Comparison',
    'Value',
  ]

  constructor(private midiService: MidiService) {

  }

  ngAfterContentChecked(): void {
    this.getRules().forEach(rule => {
      let op = 'operatorSelect-' + rule.id
      this.setSelectValue(op, rule.operatorId)
    })
    this.getRules().forEach(rule => {
      let co = 'comparisonSelect-' + rule.id
      this.setSelectValue(co, rule.comparisonId)
    })
  }

  ngAfterViewInit(): void {
  }

  ngOnInit(): void {

  }

  // lookups: Map<string, string[]> = new Map()

  onClick(rule: Rule, $event: MouseEvent) {
    console.log(rule.operatorId)
    console.log(rule.comparisonId)
    console.log(rule.value)
  }

  getRules(): Rule[] {
    return this.player == undefined ? [] : this.player.rules
  }
  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id)
    if (element != null) { // @ts-ignore
      element.selectedIndex = val
    }
  }

  onOperatorChange(rule: Rule, event: { target: any }) {
    this.midiService.updateRuleClicked(this.player.id, rule.id, this.OPERATOR.indexOf(event.target.value), rule.comparisonId, rule.value).subscribe()
    rule.operatorId=event.target.value
  }

  onComparisonChange(rule: Rule, event: { target: any }) {
    this.midiService.updateRuleClicked(this.player.id, rule.id, rule.operatorId, this.COMPARISON.indexOf(event.target.value), rule.value).subscribe()
    rule.comparisonId=event.target.value
  }

  onValueChange(rule: Rule, event: { target: any }) {
    rule.value = event.target.value
    this.midiService.updateRuleClicked(this.player.id, rule.id, rule.operatorId, rule.comparisonId, event.target.value).subscribe()
  }

  btnClicked(rule: Rule, command: string) {
    switch (command) {
      case 'add': {
        this.midiService.addRuleClicked(this.player).subscribe();
        break;
      }
      case 'remove': {
        this.midiService.removeRuleClicked(this.player, rule).subscribe();
        break;
      }
    }
  }

  initBtnClick() {
    this.midiService.addRuleClicked(this.player).subscribe();
  }
}
