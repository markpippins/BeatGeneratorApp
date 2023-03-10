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
  ports: Device[] = [];
  synths: Device[] = [];
  other: Device[] = [];
  unknown: Device[] = [];

  ngOnInit(): void {
    this.midiService.allDevices().subscribe(async data => {
      this.devices = data;
      this.ports = this.devices.filter(d => d.description.toLowerCase().indexOf('port') > -1)
      this.synths = this.devices.filter(d => d.description.toLowerCase().indexOf('synth') > -1)
      this.unknown = this.devices.filter(d => d.description.toLowerCase().indexOf('no details') > -1)
      this.other = this.devices.filter(d => !this.ports.includes(d) && ! this.synths.includes(d) && !this.unknown.includes(d))
    })
  }
}
