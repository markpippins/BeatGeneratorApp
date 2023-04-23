import { Component, Input } from '@angular/core';
import { NgStyle } from '@angular/common';
@Component({
  selector: 'app-knob',
  templateUrl: './knob.component.html',
  styleUrls: ['./knob.component.css'],
})
export class KnobComponent {
  name = 'Angular';
  _value = 0;
  min = 0;
  max = 100;

  @Input()
  identifier!: string

  set value(value) {
    /*if (value < this.min) {
      value = this.min;
    }else if(value > this.max){
      value = this.max;
    }*/
    this._value = value;
  }

  get value() {
    return this._value;
  }

  get angle() {
    return (this.value / this.max) * 360;
  }

  _isDragging = false;
  position: { left: any; width: any; top: any; height: any } | undefined;
  center: { x: any; y: any } | undefined;
  dragStart() {
    let element = document.getElementById('knob-wrapper-'+ this.identifier);
    if (element != null) {
      this._isDragging = true;
      const { top, left, width, height } = element.getBoundingClientRect();
      this.position = { top, left, width, height };
      this.center = {
        x: this.position.left + this.position.width / 2,
        y: this.position.top + this.position.height / 2,
      };
      console.log('drag start', this.position);
    }
  }

  dragEnd() {
    this._isDragging = false;
  }

  drag(event: MouseEvent | TouchEvent) {
    if (this._isDragging) {
      const clientX =
        (event as MouseEvent).clientX ||
        (event as TouchEvent).touches[0].clientX;
      const clientY =
        (event as MouseEvent).clientY ||
        (event as TouchEvent).touches[0].clientY;
      if (this.center != null) {
        const centerX = this.center.x;
        const centerY = this.center.y;
        const deltaX = clientX - centerX;
        const deltaY = clientY - centerY;
        let angle = Math.atan2(deltaY, deltaX) * (180 / Math.PI);
        /* if (angle >= 360){
        angle -= 360;
      }*/
        this.value = (angle / 360) * this.max + this.max / 4;
        console.log(angle + 90, this.value);
      }
    }
  }
}
