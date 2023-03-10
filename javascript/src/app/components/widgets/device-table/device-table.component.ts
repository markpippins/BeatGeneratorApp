import { Component, Input } from '@angular/core';
import { Device } from 'src/app/models/device';

@Component({
  selector: 'app-device-table',
  templateUrl: './device-table.component.html',
  styleUrls: ['./device-table.component.css']
})
export class DeviceTableComponent {

  @Input()
  devices!: Device[]
}
