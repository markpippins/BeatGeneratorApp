import {Component, Input} from '@angular/core';
import {Condition} from "../../models/condition";
import {Player} from "../../models/player";
import {KeyValue} from '@angular/common';

@Component({
  selector: 'app-conditions-table',
  templateUrl: './conditions-table.component.html',
  styleUrls: ['./conditions-table.component.css']
})
export class ConditionsTableComponent {

  @Input()
  player!: Player;
  conditionCols: string[] = [
    'operator',
    'comparison',
    'value',
  ];

  onClick(condition: Condition, $event: MouseEvent) {
    console.log(condition.operator)
    console.log(condition.comparison)
    console.log(condition.value)
  }

  getConditions(): Condition[] {
    if (this.player == undefined)
      return []
    return this.player.conditions;
  }
}
