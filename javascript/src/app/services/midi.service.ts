import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Player} from '../models/player';
import {Instrument} from '../models/instrument';
import {Ticker} from '../models/ticker';
import {Evaluator} from '../models/evaluator';
import {Rule} from "../models/rule";

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

  nextClicked() {
    return this.http.get('http://localhost:8080/api/ticker/next');
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

  allInstruments() {
    let params = new HttpParams();
    return this.http.get<Map<String, Instrument>>(
      'http://localhost:8080/api/instruments/info');
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
    return this.http.get<Player>('http://localhost:8080/api/players/add', {
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

  addRuleClicked(player: Player) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get<Rule>('http://localhost:8080/api/rules/add', {
      params: params,
    })
  }

  removeRuleClicked(player: Player, rule: Evaluator) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    params = params.append('ruleId', rule.id);
    return this.http.get('http://localhost:8080/api/rules/remove', {
      params: params,
    });
  }

  updatePlayerClicked(playerId: number, updateType: number, updateValue: number) {
    let params = new HttpParams();
    params = params.append('playerId', playerId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get('http://localhost:8080/api/player/update', {
      params: params,
    });
  }

  updateRuleClicked(playerId: number, ruleId: number, operatorId: number, comparisonId: number, newValue: number) {
    let params = new HttpParams();
    params = params.append('playerId', playerId);
    params = params.append('ruleId', ruleId);
    params = params.append('operatorId', operatorId);
    params = params.append('comparisonId', comparisonId);
    params = params.append('newValue', newValue);
    return this.http.get('http://localhost:8080/api/rule/update', {
      params: params,
    });
  }
}
