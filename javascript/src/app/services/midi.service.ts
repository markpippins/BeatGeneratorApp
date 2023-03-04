import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Player} from '../models/player';
import {Instrument} from '../models/instrument';
import {Ticker} from '../models/ticker';
import {Evaluator} from '../models/evaluator';
import {Rule} from "../models/rule";
import {StepData} from "../models/step-data";

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

  start() {
    return this.http.get('http://localhost:8080/api/ticker/start');
  }

  next(currentTickerId: number) {
    let params = new HttpParams();
    params = params.append('currentTickerId', currentTickerId);
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/next', {params});
  }

  previous(currentTickerId: number) {
    let params = new HttpParams();
    params = params.append('currentTickerId', currentTickerId);
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/previous', {params});
  }

  stop() {
    return this.http.get('http://localhost:8080/api/ticker/stop');
  }

  pause() {
    return this.http.get('http://localhost:8080/api/ticker/pause');
  }

  playerInfo() {
    return this.http.get<Player[]>('http://localhost:8080/api/players/info');
  }

  tickerInfo() {
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/info');
  }

  tickerStatus() {
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/status');
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

  record() {
    return this.http.get('http://localhost:8080/api/players/clear');
  }

  addPlayer() {
    let params = new HttpParams();
    params = params.append('instrument', 'blackbox');
    return this.http.get<Player>('http://localhost:8080/api/players/add', {
      params: params,
    });
  }

  removePlayer(player: Player) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get<Player[]>('http://localhost:8080/api/players/remove', {
      params: params,
    });
  }

  addRule(player: Player) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get<Rule>('http://localhost:8080/api/rules/add', {
      params: params,
    })
  }

  removeRule(player: Player, rule: Evaluator) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    params = params.append('ruleId', rule.id);
    return this.http.get('http://localhost:8080/api/rules/remove', {
      params: params,
    });
  }

  updatePlayer(playerId: number, updateType: number, updateValue: number) {
    let params = new HttpParams();
    params = params.append('playerId', playerId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get('http://localhost:8080/api/player/update', {
      params: params,
    });
  }

  updateRule(playerId: number, ruleId: number, operatorId: number, comparisonId: number, newValue: number) {
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

  refresh() {
    return this.http.get('http://localhost:8080/api/ticker/info');
  }

  addTrack(steps: StepData[]) {
    return this.http.post<StepData[]>('http://localhost:8080/api/sequence/play', steps);
  }

  updateStep(step: StepData) {
    return this.http.post<StepData>('http://localhost:8080/api/steps/update', step);
  }
}
