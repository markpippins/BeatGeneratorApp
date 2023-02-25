import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Player} from '../models/player';
import {subscribeOn} from 'rxjs';
import {Instrument} from '../models/instrument';
import {Ticker} from '../models/ticker';
import {Condition} from '../models/condition';
import {ConditionUpdate} from '../models/condition-update';

@Injectable({
  providedIn: 'root',
})
export class MidiService {
  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      responseType: 'text',
    }),
  };

  URL!: string;

  constructor(public http: HttpClient) {
    this.URL = 'http://localhost:8080/api';
  }

  delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  startClicked() {
    return this.http.get('http://localhost:8080/api/ticker/start');
  }

  stopClicked() {
    return this.http.get('http://localhost:8080/api/ticker/stop');
  }

  pauseClicked() {
    return this.http.get('http://localhost:8080/api/ticker/pause');
  }

  playerInfo() {
    return this.http.get<Player[]>('http://localhost:8080/api/players/info');
  }

  tickerInfo() {
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/info');
  }

  instrumentInfo(channel: number) {
    let params = new HttpParams();
    params = params.append('channel', channel);
    return this.http.get<Instrument>(
      'http://localhost:8080/api/instrument/info',
      {params: params}
    );
  }

  clearPlayers() {
    return this.http.get('http://localhost:8080/api/players/clear');
  }

  playNote(instrument: string, channel: number, note: number) {
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    params = params.append('channel', channel);
    params = params.append('note', note);
    return this.http
      .get('http://localhost:8080/api/drums/note', {params: params})
      .subscribe();
  }

  sendMessage(
    messageType: number,
    channel: number,
    data1: number,
    data2: number
  ) {
    let params = new HttpParams();
    params = params.append('messageType', messageType);
    params = params.append('channel', channel);
    params = params.append('data1', data1);
    params = params.append('data2', data2);
    return this.http
      .get('http://localhost:8080/api/messages/send', {params: params})
      .subscribe();
  }

  recordClicked() {
    return this.http.get('http://localhost:8080/api/players/clear');
  }

  addPlayerClicked() {
    let params = new HttpParams();
    params = params.append('instrument', 'blackbox');
    return this.http.get('http://localhost:8080/api/players/add', {
      params: params,
    });
  }

  removePlayerClicked(player: Player) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get('http://localhost:8080/api/players/remove', {
      params: params,
    });
  }

  addConditionClicked(player: Player) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get('http://localhost:8080/api/conditions/add', {
      params: params,
    });
  }
}


  // updateConditionClicked(
//   playerCondition: PlayerCondition,
//   newOperator: string,
//   newComparison: string,
//   newValue: number
// ) {
//   let params = new HttpParams();
//   params = params.append('playerId', playerCondition.playerId);
//   params = params.append('oldOperator', playerCondition.operator);
//   params = params.append('newOperator', newOperator);
//   params = params.append('oldComparison', playerCondition.comparison);
//   params = params.append('newComparison', newComparison);
//   params = params.append('oldValue', playerCondition.value);
//   params = params.append('newValue', newValue);
//
//   return this.http.get('http://localhost:8080/api/conditions/update', {
//     params: params,
//   });
