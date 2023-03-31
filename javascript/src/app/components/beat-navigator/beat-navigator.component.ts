import { Component, Input } from '@angular/core';
import { Ticker } from 'src/app/models/ticker';

@Component({
  selector: 'app-beat-navigator',
  templateUrl: './beat-navigator.component.html',
  styleUrls: ['./beat-navigator.component.css']
})
export class BeatNavigatorComponent {
  @Input()
  ticker!: Ticker;
}
