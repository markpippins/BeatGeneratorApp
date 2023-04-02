import { Injectable } from '@angular/core';
import { Listener } from '../models/listener';

@Injectable({
  providedIn: 'root'
})


export class UiService {

  listeners: Listener[] = []

  constructor() { }

  addListener(listener: Listener) {
    this.listeners.push(listener)
  }

  notifyAll(messageType: number, message: string, messageValue: number) {
    this.listeners.forEach(l => l.onNotify(messageType, message, messageValue))
  }

  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id)
    if (element != null) { // @ts-ignore
      element.selectedIndex = val
    }
  }

  toggleClass(el: any, className: string) {
    if (el.className.indexOf(className) >= 0) {
      el.className = el.className.replace(className, "");
    } else {
      el.className += className;
    }
  }


  swapClass(el: any, classNameA: string, classNameB: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, classNameB);
    } else if (el.className.indexOf(classNameB) >= 0) {
      el.className = el.className.replace(classNameB, classNameA);
    }
  }

  removeClass(el: any, classNameA: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, "");
    }
  }

  reverseSortById(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.id < b.id) {
        return 1;
      }
      if (a.id > b.id) {
        return -1;
      }
      return 0;
    });
  }

  sortById(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.id > b.id) {
        return 1;
      }
      if (a.id < b.id) {
        return -1;
      }
      return 0;
    });
  }

  sortByName(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.name > b.name) {
        return 1;
      }
      if (a.name < b.name) {
        return -1;
      }
      return 0;
    });
  }

  sortByPosition(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.position > b.position) {
        return 1;
      }
      if (a.position < b.position) {
        return -1;
      }
      return 0;
    });
  }
}
