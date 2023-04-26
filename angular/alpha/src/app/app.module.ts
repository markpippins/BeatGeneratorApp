import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { SliderPanelComponent } from './components/slider-panel/slider-panel.component';
import { ChannelSelectorComponent } from './components/widgets/channel-selector/channel-selector.component';
import { InstrumentSelectorComponent } from './components/widgets/instrument-selector/instrument-selector.component';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { PadsPanelComponent } from './components/widgets/pads-panel/pads-panel.component';
import { DrumPadComponent } from './components/widgets/drum-pad/drum-pad.component';
import { StatusPanelComponent } from './components/status-panel/status-panel.component';
import { MatRadioModule } from '@angular/material/radio';
import { TransportControlComponent } from './components/widgets/transport-control/transport-control.component';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { DrumGridComponent } from './components/drum-grid/drum-grid.component';
import { PlayerTableComponent } from './components/player-table/player-table.component';
import { MatTableModule } from '@angular/material/table';
import { SliderComponent } from './components/widgets/slider/slider.component';
import { MatSliderModule } from '@angular/material/slider';
import { BeatSpecComponent } from './components/beat-spec/beat-spec.component';
import { BeatSpecPanelComponent } from './components/beat-spec-panel/beat-spec-panel.component';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { RuleTableComponent } from './components/rule-table/rule-table.component';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';
import { DevicePanelComponent } from './components/device-panel/device-panel.component';
import { DeviceTableComponent } from './components/widgets/device-table/device-table.component';
import { RandomizerPanelComponent } from './components/widgets/randomizer-panel/randomizer-panel.component';
import { ControlPanel808Component } from './components/widgets/control-panel808/control-panel808.component';
import { ParamsPanel808Component } from './components/widgets/params-panel808/params-panel808.component';
import { StrikeDetailComponent } from './components/widgets/strike-detail/strike-detail.component';
import { BeatSpecDetailComponent } from './components/beat-spec-detail/beat-spec-detail.component';
import { MidinInstrumentComboComponent } from './components/widgets/midin-instrument-combo/midin-instrument-combo.component';
import { BeatNavigatorComponent } from './components/beat-navigator/beat-navigator.component';
import { TickerNavComponent } from './components/widgets/ticker-nav/ticker-nav.component';
import { SetNavComponent } from './components/widgets/set-nav/set-nav.component';
import { RulesNavComponent } from './components/widgets/rules-nav/rules-nav.component';
import { ButtonPanelComponent } from './components/widgets/button-panel/button-panel.component';
import { PlayerNavComponent } from './components/widgets/player-nav/player-nav.component';
import { SocketIoModule, SocketIoConfig } from 'ngx-socket-io';
import { StatusReadoutComponent } from './components/widgets/status-readout/status-readout.component';
import { CommonModule } from '@angular/common';
import { NgStyle } from '@angular/common';
import { KnobModule } from "primeng/knob";
import { CheckboxModule } from 'primeng/checkbox';
import { MidiKnobComponent } from './components/widgets/midi-knob/midi-knob.component';
import { LaunchpadComponent } from './components/widgets/launchpad/launchpad.component';
import { MiniSliderPanelComponent } from './widgets/mini-slider-panel/mini-slider-panel.component';
const config: SocketIoConfig = { url: 'http://localhost:8988', options: {} };

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    SliderPanelComponent,
    ChannelSelectorComponent,
    InstrumentSelectorComponent,
    PadsPanelComponent,
    DrumPadComponent,
    StatusPanelComponent,
    TransportControlComponent,
    DrumGridComponent,
    PlayerTableComponent,
    SliderComponent,
    BeatSpecComponent,
    BeatSpecPanelComponent,
    RuleTableComponent,
    DevicePanelComponent,
    DeviceTableComponent,
    RandomizerPanelComponent,
    ControlPanel808Component,
    ParamsPanel808Component,
    StrikeDetailComponent,
    BeatSpecDetailComponent,
    MidinInstrumentComboComponent,
    BeatNavigatorComponent,
    TickerNavComponent,
    SetNavComponent,
    RulesNavComponent,
    ButtonPanelComponent,
    PlayerNavComponent,
    StatusReadoutComponent,
    MidiKnobComponent,
    LaunchpadComponent,
    MiniSliderPanelComponent,
  ],
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MatSelectModule,
    FormsModule,
    MatRadioModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    KnobModule,
    CheckboxModule,
    MatSliderModule,
    MatButtonModule,
    MatTabsModule,
    MatCheckboxModule,
    MatListModule,
    MatToolbarModule,
    MatSliderModule,
    SocketIoModule.forRoot(config),
    NgStyle,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
