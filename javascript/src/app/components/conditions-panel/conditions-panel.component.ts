import {Component, Input} from '@angular/core';
import {Player} from "../../models/player";
import {Condition} from "../../models/condition";

@Component({
  selector: 'app-conditions-panel',
  templateUrl: './conditions-panel.component.html',
  styleUrls: ['./conditions-panel.component.css']
})
export class ConditionsPanelComponent {
  @Input()
  player!: Player;
}
