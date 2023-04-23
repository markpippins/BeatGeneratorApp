import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-conditions-select-option',
  templateUrl: './conditions-select-option.component.html',
  styleUrls: ['./conditions-select-option.component.css']
})
export class ConditionsSelectOptionComponent {
  @Input()
  value!: string;

}
