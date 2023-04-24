import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Instrument } from '../../models/instrument';
import { MidiService } from '../../services/midi.service';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';
import { Panel } from 'src/app/models/panel';

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css'],
})
export class SliderPanelComponent implements OnInit, Listener {
  @Input()
  channel = 1;

  value5: number = 50;

  pnls: Map<string, Panel[]> = new Map();

  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Input()
  instrument!: Instrument | undefined;

  // @Input()
  instruments!: Instrument[];

  @Output()
  configModeOn = false;

  constructor(private midiService: MidiService, private uiService: UiService) {}

  onNotify(_messageType: number, _message: string) {}

  onSelect(selectedChannel: number) {
    this.instrument = undefined;
    this.channel = selectedChannel;
    this.midiService
      .instrumentInfoByChannel(selectedChannel - 1)
      .subscribe(async (data) => {
        this.instrument = data;
        this.channelSelectEvent.emit(selectedChannel);
      });
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
    this.onSelect(10);
    this.midiService.allInstruments().subscribe(async (data) => {
      this.instruments = this.uiService.sortByName(data);
      this.buildPanelMap(this.instruments);
    });
  }

  getRangeColor() {
    return 'lightblue';
  }

  getStrokeWidth() {
    return 10;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'black';
  }

  getValueColor() {
    return 'fuchsia';
  }

  getValueTemplate(_name: string) {
    return _name
  }

  buildPanelMap(instruments: Instrument[]) {
    instruments.forEach((instrument) => {
      this.pnls.set(
        instrument.name,
        this.createMap(instrument.controlCodes.map((cc) => cc.name))
      );
    });
  }

  getPanelsForInstrument(name: string): string[] {
    let result: string[] = [];

    this.pnls.get(name)?.forEach((pnl) => {
      if (!result.includes(pnl.name)) result.push(pnl.name.replace('(', ''));
    });

    return result.sort();
  }

  getOtherControlCodes(name: string): String[] {
    let result: string[] = [];
    this.pnls.get(name)?.forEach((pnl) => {
      if (pnl.name == 'Other')
        pnl.children.forEach((child) => {
          let childName = child.name.replace('Other ', '');
          if (!result.includes(childName)) result.push(childName);
        });
    });

    return result.sort();
  }

  getControlCodes(instrument: Instrument, search: string): string[] {
    return instrument.controlCodes.filter((cc) => cc.name.startsWith(search)).map(o => o.name.replace(search, ''));
  }

  configBtnClicked() {
    this.configModeOn = !this.configModeOn;
  }

  createMap(data: string[]): Panel[] {
    const map: Panel[] = [];

    for (const name of data) {
      const parts = name.split(' ');

      let parent = map.find((panel) => panel.name === parts[0]);

      if (!parent) {
        parent = { name: parts[0], children: [] };
        map.push(parent);
      }

      let child = parent.children.find((panel) => panel.name === name);

      if (!child) {
        child = { name, children: [] };
        parent.children.push(child);
      }
    }

    for (const parent of map) {
      if (parent.children.length === 1) {
        const child = parent.children[0];
        parent.name = 'Other';
        child.name = `${parent.name} ${child.name}`;
        parent.children.push(child);
      } else {
        const common = this.findCommonPrefix(
          parent.children.map((panel) => panel.name)
        );
        parent.name = common || parent.name;
      }

      // let changeChildren = false;
      // for (const child of parent.children)
      //   if (child.name.length > 10) changeChildren = true;
      // if (changeChildren)
      //   for (const child of parent.children)
      //     child.name = child.name.replace(parent.name, '');
    }

    return map;
  }

  findCommonPrefix(strings: string[]): string {
    if (strings.length === 0) {
      return '';
    }

    let prefix = strings[0];

    for (const string of strings) {
      let i = 0;

      while (
        i < prefix.length &&
        i < string.length &&
        prefix[i] === string[i]
      ) {
        i++;
      }

      prefix = prefix.slice(0, i);
    }

    return prefix;
  }
}
