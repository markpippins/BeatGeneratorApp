import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DashboardComponent } from './routes/dashboard/dashboard.component';
import { SliderPanelComponent } from './components/slider-panel/slider-panel.component';
import { ChannelSelectorComponent } from './components/widgets/channel-selector/channel-selector.component';
import { InstrumentSelectorComponent } from './components/widgets/instrument-selector/instrument-selector.component';
import {MatSelectModule} from "@angular/material/select";
import {FormsModule} from "@angular/forms";
import { PadsPanelComponent } from './components/pads-panel/pads-panel.component';
import { DrumPadComponent } from './components/widgets/drum-pad/drum-pad.component';
import { StatusPanelComponent } from './components/status-panel/status-panel.component';
import {MatRadioModule} from "@angular/material/radio";
import { TransportControlComponent } from './components/widgets/transport-control/transport-control.component';
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import { DrumGridComponent } from './components/drum-grid/drum-grid.component';
import { PlayerPanelComponent } from './components/player-panel/player-panel.component';
import { PlayerTableComponent } from './components/player-table/player-table.component';
import {MatTableModule} from "@angular/material/table";
import { ConditionsPanelComponent } from './components/conditions-panel/conditions-panel.component';
import { ConditionsSelectOptionComponent } from './components/widgets/conditions-select-option/conditions-select-option.component';
import { SliderComponent } from './components/widgets/slider/slider.component';
import {NgxSliderModule} from "@angular-slider/ngx-slider";
import {MatSliderModule} from "@angular/material/slider";
import { BeatSpecComponent } from './components/beat-spec/beat-spec.component';
import { BeatSpecPanelComponent } from './components/beat-spec-panel/beat-spec-panel.component';
import {MatButtonModule} from "@angular/material/button";
import {MatTabsModule} from "@angular/material/tabs";
import { RuleTableComponent } from './components/rule-table/rule-table.component';
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatListModule} from "@angular/material/list";
import { InstrumentSelectDialogComponent } from './components/instrument-select-dialog/instrument-select-dialog.component';
import {MatToolbarModule} from "@angular/material/toolbar";
import { DevicePanelComponent } from './components/device-panel/device-panel.component';
import { DeviceTableComponent } from './components/widgets/device-table/device-table.component';
import { RandomizerPanelComponent } from './components/widgets/randomizer-panel/randomizer-panel.component';
import { ControlPanel808Component } from './components/widgets/control-panel808/control-panel808.component';
import { ParamsPanel808Component } from './components/widgets/params-panel808/params-panel808.component';
import { StrikeDetailComponent } from './components/widgets/strike-detail/strike-detail.component';
import { BeatSpecDetailComponent } from './components/beat-spec-detail/beat-spec-detail.component';
import { MidinInstrumentComboComponent } from './components/widgets/midin-instrument-combo/midin-instrument-combo.component';

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
    PlayerPanelComponent,
    PlayerTableComponent,
    ConditionsPanelComponent,
    ConditionsSelectOptionComponent,
    SliderComponent,
    BeatSpecComponent,
    BeatSpecPanelComponent,
    RuleTableComponent,
    InstrumentSelectDialogComponent,
    DevicePanelComponent,
    DeviceTableComponent,
    RandomizerPanelComponent,
    ControlPanel808Component,
    ParamsPanel808Component,
    StrikeDetailComponent,
    BeatSpecDetailComponent,
    MidinInstrumentComboComponent,
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        MatSelectModule,
        FormsModule,
        MatRadioModule,
        MatIconModule,
        MatInputModule,
        MatTableModule,
        NgxSliderModule,
        MatSliderModule,
        MatButtonModule,
        MatTabsModule,
        MatCheckboxModule,
        MatListModule,
        MatToolbarModule
    ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
