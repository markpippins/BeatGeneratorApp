import { Component, Input, OnInit, Output } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { Comparison } from 'src/app/models/comparison';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Listener } from 'src/app/models/listener';
import { Operator } from 'src/app/models/operator';
import { Player } from 'src/app/models/player';
import { RuleUpdateType } from 'src/app/models/rule-update-type';
import { Ticker } from 'src/app/models/ticker';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-beat-navigator',
  templateUrl: './beat-navigator.component.html',
  styleUrls: ['./beat-navigator.component.css'],
})
export class BeatNavigatorComponent implements OnInit, Listener {
  @Input()
  ticker!: Ticker;

  divCount = 16;
  colCount = 24;

  ticksPosition = this.colCount;
  tickRange: number[] = [];
  tickOverflow: string[] = [];
  ticks: number[] = [];
  beats: number[] = [];
  bars: number[] = [];
  divs: number[] = [];
  parts: number[] = [];
  range: number[] = [];

  selectedTicks: boolean[] = [];
  selectedBeats: boolean[] = [];
  selectedBars: boolean[] = [];
  selectedDivs: boolean[] = [];
  selectedParts: boolean[] = [];
  selectedNote: number = 0;
  resolution: string[] = ['accent', 'tick', 'beat', 'bar', 'part', 'div'];

  comboId = 'beat-navigator';
  controlBtnClassName = 'mini-control-btn';

  instruments!: Instrument[];

  constructor(private uiService: UiService, private midiService: MidiService) {
    uiService.addListener(this);
  }

  generate() {
    if (this.selectedNote == undefined)
      return

    let beatIndex = 0;
    this.selectedBeats.forEach((beat) => {
      if (beat) {
        let beatValue = beatIndex + 1;

        if (this.selectedTicks.includes(true)) {
          let tickIndex = 0;
          this.selectedTicks.forEach((tick) => {
            if (tick) {
              let tickValue = tickIndex + 1;
              this.midiService.addPlayerForNote(this.selectedNote).subscribe((player) => {
                this.addRuleForTick(player, tickValue);
                this.addRuleForBeat(player, beatValue);

                let barIndex = 0;
                this.selectedBars.forEach((bar) => {
                  if (bar) {
                    let barValue = barIndex + 1;
                    this.addRuleForBar(player, barValue);
                  }
                  barIndex++;
                });

                let partIndex = 0;
                this.selectedBars.forEach((bar) => {
                  if (bar) {
                    let partValue = partIndex + 1;
                    this.addRuleForPart(player, partValue);
                  }
                  partIndex++;
                });
              });
            }
            tickIndex++;
          });
        } else {
          this.midiService.addPlayer().subscribe((player) => {
            this.addRuleForBeat(player, beatValue);
          });
        }
      }
      beatIndex++;
    });
  }
  // this.midiService.addPlayer().subscribe((player) => {
  //   this.addRuleForBeat(player, beatValue);
  //   if (!this.selectedTicks.includes(true))
  //     this.addRuleForTick(player, 1);
  //   else {
  //     let tickIndex = 0;
  //     this.selectedTicks.forEach((tick) => {
  //       if (tick) {
  //         let tickValue = tickIndex + 1;
  //         this.addRuleForTick(player, tickValue);
  //       }
  //       tickIndex++;
  //     });
  //   }
  // });

  // }
  // });

  // while (beatIndex < this.selectedBeats.length) {
  //   if (this.selectedBeats[beatIndex]) {
  //     this.midiService.addPlayer().subscribe((player) => {
  //       // let tickIndex = 0;
  //       // this.ticks.forEach((tick) => {
  //       //   if (this.selectedTicks[tickIndex])
  //       this.addRuleForBeat(player, beatIndex);
  //       // this.addRuleForTick(player, tickIndex);
  //       // tickIndex++;
  //       // });
  //     });
  //   }
  // }

  addRuleForTick(player: Player, tick: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.TICK, Comparison.EQUALS, tick, 0)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForBeat(player: Player, beat: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.BEAT, Comparison.EQUALS, beat, 0)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForBar(player: Player, bar: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.BAR, Comparison.EQUALS, bar, 0)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForPart(player: Player, part: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.PART, Comparison.EQUALS, part, 0)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  // this.bars.forEach((bar) => {
  //   if (this.selectedBars[bar])
  //     this.midiService
  //       .addSpecifiedRule(
  //         player,
  //         Operator.BAR,
  //         Comparison.EQUALS,
  //         bar + 1,
  //         0
  //       )
  //       .subscribe((data) =>
  //       this.uiService.notifyAll(
  //         Constants.PLAYER_UPDATED,
  //         'Rule Added',
  //         data.id
  //       ));
  // });

  // this.parts.forEach((part) => {
  //   if (this.selectedParts[part])
  //     this.midiService
  //       .addSpecifiedRule(
  //         player,
  //         Operator.PART,
  //         Comparison.EQUALS,
  //         part,
  //         0
  //       )
  //       .subscribe((data) =>
  //       this.uiService.notifyAll(
  //         Constants.PLAYER_UPDATED,
  //         'Rule Added',
  //         data.id
  //       ));
  // });

  // this.midiService
  //   .addSpecifiedRule(
  //     player,
  //     Operator.BEAT,
  //     Comparison.EQUALS,
  //     beat + 1,
  //     0
  //   )
  //   .subscribe();

  // this.selectedTicks.forEach;

  getBeatPerBar(): number {
    return this.ticker == undefined ? 4 : this.ticker.beatsPerBar;
  }

  ngOnInit(): void {
    this.updateDisplay();
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
      this.uiService.setSelectValue('instrument-combo-' + this.comboId, 0);
    });
  }

  onInstrumentSelected(instrument: Instrument) {
    this.range = [];
    if (
      instrument.lowestNote > 0 &&
      instrument.highestNote > instrument.lowestNote
    )
      for (
        let note = instrument.lowestNote;
        note < instrument.highestNote + 1;
        note++
      ) {
        this.range.push(note);
      }
  }

  onNotify(messageType: number, message: string, messageValue: number) {
    if (messageType == Constants.TICKER_UPDATED) this.updateDisplay();

    if (messageType == Constants.BEAT_DIV) {
      let name = 'mini-beat-btn-' + messageValue;
      let element = document.getElementById(name);
      if (element != undefined)
        this.uiService.swapClass(element, 'inactive', 'active');

      // this.beats.filter(b => b != messageValue).forEach(b => {
      //   let name = "mini-beat-btn-" + messageValue
      //   let element = document.getElementById(name)
      //   if (element != undefined)
      //     this.uiService.swapClass(element, 'active', 'inactive')

      // })
    }
  }

  updateDisplay() {
    this.midiService.tickerInfo().subscribe((data) => {
      this.ticksPosition = this.colCount;

      this.ticker = data;
      this.ticks = [];
      this.beats = [];
      this.bars = [];
      this.divs = [];
      this.parts = [];
      // this.selectedTicks = []
      // this.selectedBeats = []
      // this.selectedBars = []
      // this.selectedDivs = []
      // this.selectedParts = []

      this.tickRange = [];
      this.tickOverflow = [];

      for (let index = 0; index < this.ticker.ticksPerBeat; index++) {
        this.ticks.push(index + 1);
        this.selectedTicks.push(false);
        if (this.tickRange.length < this.colCount) this.tickRange.push(index);
      }

      while (this.tickRange.length + this.tickOverflow.length < this.colCount)
        this.tickOverflow.push('');

      for (let index = 0; index < this.ticker.beatsPerBar; index++) {
        this.beats.push(index + 1);
        this.selectedBeats.push(false);
      }

      for (let index = 0; index < this.divCount; index++) {
        this.divs.push(index + 1);
        this.selectedDivs.push(false);
      }

      for (let index = 0; index < this.ticker.bars; index++) {
        this.bars.push(index + 1);
        this.selectedBars.push(false);
      }

      for (let index = 0; index < this.ticker.parts; index++) {
        this.parts.push(index + 1);
        this.selectedParts.push(false);
      }
    });

    // this.updateSelections()
  }

  // updateSelections() {
  //   let index = 0
  //   this.selectedTicks.forEach(t => {
  //     let name = "mini-tick-btn-" + index
  //     let element = document.getElementById(name)
  //     if (element != undefined)
  //       this.uiService.addClass(element, "mini-btn-selected")
  //   })
  // }

  onNoteClicked(note: number, event: Event) {
    this.range.forEach((note) => {
      let element = document.getElementById(
        'mini-note-btn-' + this.selectedNote
      );
      if (element != undefined)
        this.uiService.removeClass(element, '-selected');
    });

    this.selectedNote = note;
    this.uiService.swapClass(event.target, 'mini-btn-selected', 'mini-btn');
  }

  getNote(value: number): string {
    return this.uiService.getNoteForValue(value);
  }

  getAccentsAsStrings(): string[] {
    let accents: string[] = [];
    this.ticks.forEach((t) => accents.push('𝄈'));
    return accents;
  }

  getTickRangeAsStrings() {
    return this.tickRange.map((tr) => String(tr));
  }

  getTicksAsStrings(): string[] {
    return this.ticks.map(String);
  }

  getBeatsAsStrings(): string[] {
    return this.beats.map(String);
  }

  getBarsAsStrings(): string[] {
    return this.bars.map(String);
  }

  getPartsAsStrings(): string[] {
    return this.parts.map(String);
  }

  tickSelected(index: number) {
    this.selectedTicks[index] = !this.selectedTicks[index];
  }

  beatSelected(index: number) {
    this.selectedBeats[index] = !this.selectedBeats[index];
  }

  barSelected(index: number) {
    this.selectedBars[index] = !this.selectedBars[index];
  }

  partSelected(index: number) {
    this.selectedParts[index] = !this.selectedParts[index];
  }
}
