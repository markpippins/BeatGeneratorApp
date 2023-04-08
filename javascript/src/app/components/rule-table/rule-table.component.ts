import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MidiService } from '../../services/midi.service';
import { Player } from '../../models/player';
import { Rule } from '../../models/rule';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';
import { RuleUpdateType } from 'src/app/models/rule-update-type';
import { Listener } from 'src/app/models/listener';

@Component({
  selector: 'app-rule-table',
  templateUrl: './rule-table.component.html',
  styleUrls: ['./rule-table.component.css'],
})
export class RuleTableComponent implements Listener {
  @Output()
  ruleChangeEvent = new EventEmitter<Player>();
  EQUALS = 0;
  GREATER_THAN = 1;
  LESS_THAN = 2;
  MODULO = 3;
  COMPARISON = ['=', '>', '<', '%', '*', '/'];
  TICK = 0;
  BEAT = 1;
  BAR = 2;
  PART = 3;
  POSITION = 4;
  OPERATOR = [
    'Tick',
    'Beat',
    'Bar',
    'Part',
    'Whole Beat',
    'Ticks',
    'Beats',
    'Bars',
    'Parts',
  ];

  interval = 0.25;

  @Input()
  player!: Player;
  ruleCols: string[] = [
    // 'ID',
    'Operator',
    'Op',
    'Value',
    'Part',
  ];

  intervalSet = false;

  constructor(private midiService: MidiService, private uiService: UiService) {
    this.uiService.addListener(this);
  }
  onNotify(_messageType: number, _message: string, _messageValue: number) {
    if (_messageType == Constants.COMMAND) {
      switch (_message) {
        case 'rule-add': {
          this.midiService.addRule(this.player).subscribe(async (data) => {
            this.player.rules.push(data);
            this.ruleChangeEvent.emit(this.player);
          });
          break;
        }
      }
    }
  }

  ngAfterContentChecked(): void {
    this.getRules().forEach((rule) => {
      let op = 'operatorSelect-' + rule.id;
      this.uiService.setSelectValue(op, rule.operator);
    });
    this.getRules().forEach((rule) => {
      let co = 'comparisonSelect-' + rule.id;
      this.uiService.setSelectValue(co, rule.comparison);
    });

    // if (!this.intervalSet)
    //   this.updateDisplay();
  }

  // updateDisplay(): void {
  //   this.midiService.tickerStatus().subscribe(data => {
  //       this.interval = 1 / data.ticksPerBeat
  //       this.intervalSet = true;
  //     });
  // }

  getRules(): Rule[] {
    return this.player == undefined ? [] : this.player.rules;
  }

  onOperatorChange(rule: Rule, event: { target: any }) {
    let value = this.OPERATOR.indexOf(event.target.value);
    this.midiService
      .updateRule(rule.id, RuleUpdateType.OPERATOR, value)
      .subscribe();
    rule.operator = value;
    // let op = 'operatorSelect-' + rule.id
    this.uiService.setSelectValue(event.target, value);
    this.uiService.notifyAll(
      Constants.STATUS,
      this.OPERATOR[value] + ' selected.',
      0
    );
  }

  onComparisonChange(rule: Rule, event: { target: any }) {
    let value = this.COMPARISON.indexOf(event.target.value);
    this.midiService
      .updateRule(rule.id, RuleUpdateType.COMPARISON, value)
      .subscribe();
    rule.comparison = value;
    this.uiService.setSelectValue(event.target, value);
    this.uiService.notifyAll(
      Constants.STATUS,
      this.COMPARISON[value] + ' selected.',
      0
    );
  }

  onValueChange(rule: Rule, event: { target: any }) {
    this.midiService
      .updateRule(rule.id, RuleUpdateType.VALUE, event.target.value)
      .subscribe();
  }

  onPartChange(rule: Rule, event: { target: any }) {
    this.midiService
      .updateRule(rule.id, RuleUpdateType.PART, event.target.value)
      .subscribe();
  }

  btnClicked(rule: Rule, command: string) {
    if (this.player.id > 0)
      switch (command) {
        case 'add': {
          this.midiService.addRule(this.player).subscribe(async (data) => {
            this.player.rules.push(data);
            this.ruleChangeEvent.emit(this.player);
          });
          break;
        }
        case 'remove': {
          this.player.rules = this.player.rules.filter((r) => r.id != rule.id);
          this.midiService.removeRule(this.player, rule).subscribe();
          this.ruleChangeEvent.emit(this.player);
          break;
        }
      }
  }

  initBtnClick() {
    if (this.player.id > 0)
      this.midiService.addRule(this.player).subscribe(async (data) => {
        this.player.rules.push(data);
      });
  }
}
