import {AfterContentChecked, AfterViewInit, Component, Input, OnInit} from '@angular/core'
import {Condition} from "../../models/condition"
import {Player} from "../../models/player"

@Component({
  selector: 'app-conditions-table',
  templateUrl: './conditions-table.component.html',
  styleUrls: ['./conditions-table.component.css']
})
export class ConditionsTableComponent implements OnInit, AfterViewInit, AfterContentChecked {
  ngAfterContentChecked(): void {
    this.getConditions().forEach(condition => {
      let op = 'operatorSelect-' + condition.id
      this.setSelectValue(op, this.getOperatorSelectionIndex(condition))
    })
    this.getConditions().forEach(condition => {
      let co = 'comparisonSelect-' + condition.id
      this.setSelectValue(co, this.getComparisonSelectionIndex(condition))
    })
  }

  ngAfterViewInit(): void {
  }

  ngOnInit(): void {
    this.lookups.set('operator', [
      'TICK',
      'POSITION',
      'BEAT',
      'BAR',
      'PART',
      'RANDOM',
    ])
    this.lookups.set('comparison', [
      'GREATER_THAN',
      'EQUALS',
      'LESS_THAN',
      'MODULO',
    ])
  }

  @Input()
  player!: Player
  conditionCols: string[] = [
    'Operator',
    'Comparison',
    'Value',
  ]


  lookups: Map<string, string[]> = new Map()

  onClick(condition: Condition, $event: MouseEvent) {
    console.log(condition.operator)
    console.log(condition.comparison)
    console.log(condition.value)
  }

  getConditions(): Condition[] {
    return this.player == undefined ? [] : this.player.conditions
  }

  getLookupItems(col: string) {
    return this.lookups.has(col) ? this.lookups.get(col) : []
  }

  getOperators() {
    return this.getLookupItems('operator')
  }

  getComparisons() {
    return this.getLookupItems('comparison')
  }

  getOperatorSelectionIndex(condition: Condition) {
    // @ts-ignore
    return this.getOperators().indexOf(condition.operator)
  }

  getComparisonSelectionIndex(condition: Condition) {
    // @ts-ignore
    return this.getComparisons().indexOf(condition.comparison)
  }

  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id)
    if (element != null) { // @ts-ignore
      element.selectedIndex = val
    }
  }

  onOperatorChange(condition: Condition, event: { target: any }) {
    condition.operator = event.target.value
  }

  onComparisonChange(condition: Condition, event: { target: any }) {
    condition.comparison = event.target.value
  }
}
