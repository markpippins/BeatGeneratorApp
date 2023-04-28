import { Component, Input } from '@angular/core';
import { ControlCode } from 'src/app/models/control-code';

@Component({
  selector: 'app-captions-table',
  templateUrl: './captions-table.component.html',
  styleUrls: ['./captions-table.component.css'],
})
export class CaptionsTableComponent {
  @Input()
  controlCode!: ControlCode;

  getRowClass(_caption: string) {
    return 'table-row';
  }
}
