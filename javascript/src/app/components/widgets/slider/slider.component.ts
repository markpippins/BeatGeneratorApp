import {Component, Input} from '@angular/core';
import {Options} from "@angular-slider/ngx-slider";

@Component({
  selector: 'app-slider',
  templateUrl: './slider.component.html',
  styleUrls: ['./slider.component.css']
})
export class SliderComponent {
  @Input()
  channel!: number;

  @Input()
  cc!: number;

  @Input()
  label!: string;
  value: number = 0;
  options: Options = {
    floor: 1,
    ceil: 127,
    vertical: true,
    hideLimitLabels: true,
  };
}
