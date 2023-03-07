import {Component, OnInit} from '@angular/core';
import {Device} from "../../models/device";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-device-panel',
  templateUrl: './device-panel.component.html',
  styleUrls: ['./device-panel.component.css']
})
export class DevicePanelComponent implements  OnInit {

  constructor(private midiService: MidiService) {
  }
  devices!: Device[]

  ngOnInit(): void {
    this.midiService.allDevices().subscribe(async data => {
      this.devices = data;
    })
  }
}
