import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
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
import { ConditionsTableComponent } from './components/conditions-table/conditions-table.component';
import { ConditionsSelectOptionComponent } from './components/widgets/conditions-select-option/conditions-select-option.component';
import { SliderComponent } from './components/widgets/slider/slider.component';
import {NgxSliderModule} from "@angular-slider/ngx-slider";
import {MatSliderModule} from "@angular/material/slider";

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
    ConditionsTableComponent,
    ConditionsSelectOptionComponent,
    SliderComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    MatSelectModule,
    FormsModule,
    MatRadioModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    NgxSliderModule,
    MatSliderModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
