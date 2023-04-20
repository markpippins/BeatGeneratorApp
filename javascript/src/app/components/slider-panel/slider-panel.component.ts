import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Instrument } from '../../models/instrument';
import { MidiService } from '../../services/midi.service';
import { Listener } from 'src/app/models/listener';
import { UiService } from 'src/app/services/ui.service';
import { ControlCode } from 'src/app/models/control-code';

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css'],
})
export class SliderPanelComponent implements OnInit, Listener {
  @Input()
  channel = 1;

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
      this.buildPool(this.instruments);
    });
  }

  buildPool(instruments: Instrument[]) {
    // let panels = new Map<string, string[]>()

    instruments.forEach((instrument) => {
      let pool: Map<string, string[]> = new Map();

      instrument.controlCodes.forEach((cc) => {
        let splitted = cc.name.split(' ');
        let term = '';
        // let level = 0;
        splitted.forEach((s) => {
          term += s;
          if (!this.keyExistsForTerm(term, pool)) {
            pool.set(term, []);
          } else {
            let leaves = pool.get(this.getKeyForTerm(term, pool));
            if (!leaves?.includes(term))
              if (term != this.getKeyForTerm(term, pool)) leaves!.push(term);
          }
          term += ' ';
          // level++;
        });
      });

      let result: Map<string, string[]> = new Map();
      result.set('other', []);
      pool.forEach((value, key) => {
        if (value.length > 1) {
          let outliers: string[] = value.filter(
            (v) => !instrument.controlCodes.map((cc) => cc.name).includes(v)
          );
          if (outliers.length > 0) {
            outliers.forEach((out) =>
              result.set(
                out,
                value.filter((v) => !outliers.includes(v))
              )
            );
          } else if (value.length > 1) result.set(key, value);
        } else {
          result.get('other')?.push(value[0]);
        }
      });

      console.log(instrument.name);
      console.log('pool');
      console.log(pool);
      console.log('result');
      console.log(result);
    });
  }

  findShortest(data: string[]) {
    let val = data[0];
    let index = 0;

    for (let i = 0; i < data.length; i++) {
      if (data[i] > val) {
        val = data[i];
        index = i;
      }
    }

    return index;
  }

  setKeyForTerm(newKey: string, term: string, pools: Map<string, string[]>) {
    let oldVal = pools.get(this.getKeyForTerm(term, pools));
    pools.delete(this.getKeyForTerm(term, pools));
    pools.set(newKey, oldVal!);
  }

  getKeyForTerm(term: string, pools: Map<string, string[]>): string {
    let result = term;
    pools.forEach((_value, key) => {
      if (term.startsWith(key)) result = key;
    });
    return result;
  }

  keyExistsForTerm(term: string, pools: Map<string, string[]>) {
    let result = false;
    pools.forEach((_value, key) => {
      if (term.startsWith(key)) result = true;
    });
    return result;
  }

  arrayContainsStringThatStartsWith(
    search: string,
    data: string[],
    minimum: number
  ) {
    let count = 0;
    for (let i = 0; i < data.length; i++) {
      if (data[i].startsWith(search)) count++;
    }
    return count >= minimum;
  }

  getAreasFor(instrument: Instrument) {
    let panels: string[] = [];
    instrument.controlCodes.forEach((code) => {
      let splitted = code.name.includes(' ')
        ? code.name.split(' ', 1)
        : [code.name];
      let tokens: string[] = [];
      splitted.forEach((token) => {
        tokens.push(token);
        if (!panels.includes(token)) {
          panels.push(token);
        }
      });
    });

    return this.uiService.sortAlphabetically(panels);
  }

  getPanelsFor(instrument: Instrument, _area: string) {
    let panels: string[] = [];
    let controlCodes = instrument.controlCodes.map((cc) => cc.name);

    instrument.controlCodes.forEach((code) => {
      let splitted = code.name.includes(' ')
        ? code.name.split(' ', 2)
        : [code.name];

      if (
        code.name.startsWith(_area) &&
        !panels.includes(splitted.join(' '))
        // &&
        // !controlCodes.includes(splitted.join(' '))
      )
        panels.push(splitted.join(' '));
    });

    let temp: string[] = [];
    panels.forEach((p) => {
      if (controlCodes.includes(p)) {
        temp.push(p);
      }
    });

    panels = this.uiService.sortAlphabetically(temp);
    return panels;
  }

  getRemainingPanelsFor(instrument: Instrument, area: string) {
    let controlCodes = instrument.controlCodes
      .filter((cc) => cc.name.startsWith(area))
      .filter((cc) => !this.getPanelsFor(instrument, area).includes(cc.name))
      .map((cc) => cc.name);

    return controlCodes;
  }

  getCCsFor(instrument: Instrument, subPanel: string) {
    let ccs: ControlCode[] = [];

    instrument.controlCodes.forEach((code) => {
      if (code.name.startsWith(subPanel)) ccs.push(code);
    });

    return ccs;
  }

  configBtnClicked() {
    this.configModeOn = !this.configModeOn;
  }
}
