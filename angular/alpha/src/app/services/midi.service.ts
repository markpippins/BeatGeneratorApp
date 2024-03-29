import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Player} from '../models/player';
import {Instrument} from '../models/instrument';
import {Ticker} from '../models/ticker';
import {Evaluator} from '../models/evaluator';
import {Rule} from "../models/rule";
import {Step} from "../models/step";
import {LookupItem} from "../models/lookup-item";
import {Device} from "../models/device";
import { Song } from '../models/song';
import { Pattern } from '../models/pattern';
import { TickerStatus } from '../models/ticker-status';

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
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/stop');
  }

  pause() {
    return this.http.get('http://localhost:8080/api/ticker/pause');
  }

  playerInfo() {
    return this.http.get<Player[]>('http://localhost:8080/api/players/info');
  }

  songInfo() {
    return this.http.get<Song>('http://localhost:8080/api/song/info');
  }

  tickerInfo() {
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/info');
  }

  tickerStatus() {
    return this.http.get<TickerStatus>('http://localhost:8080/api/ticker/status');
  }

  allDevices() {
    return this.http.get<Device[]>(
      'http://localhost:8080/api/devices/info');
  }

  instrumentInfoByChannel(channel: number) {
    let params = new HttpParams();
    params = params.append('channel', channel);
    return this.http.get<Instrument>(
      'http://localhost:8080/api/midi/instrument',
      {params: params}
    );
  }

  instrumentInfoById(instrumentId: number) {
    let params = new HttpParams();
    params = params.append('instrumentId', instrumentId);
    return this.http.get<Instrument>(
      'http://localhost:8080/api/instrument',
      {params: params}
    );
  }

  allInstruments() {
    return this.http.get<Instrument[]>(
      'http://localhost:8080/api/instruments/all');
  }

  instrumentLookup() {
    // let params = new HttpParams();
    return this.http.get<LookupItem[]>(
      'http://localhost:8080/api/instruments/lookup');
  }

  saveConfig() {
    // let params = new HttpParams();
    return this.http.get<LookupItem[]>(
      'http://localhost:8080/api/beat/save');
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

  addPlayer(instrument: string) {
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    return this.http.get<Player>('http://localhost:8080/api/players/add', {
      params: params,
    });
  }

  addPlayerForNote(instrument: string, note: number) {
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    params = params.append('note', note);
    return this.http.get<Player>('http://localhost:8080/api/players/note', {
      params: params,
    });
  }

  newPlayer(instrumentId: number) {
    let params = new HttpParams();
    params = params.append('instrumentId', instrumentId);
    return this.http.get<Player>('http://localhost:8080/api/players/new', {
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

  addSpecifiedRule(player: Player, operator: number, comparison: number, value: number, part: number) {
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    params = params.append('operator', operator);
    params = params.append('comparison', comparison);
    params = params.append('part', part);
    params = params.append('value', value);
    return this.http.get<Rule>('http://localhost:8080/api/rules/specify', {
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
    return this.http.get<Player>('http://localhost:8080/api/player/update', {
      params: params,
    });
  }

  updatePattern(patternId: number, updateType: number, updateValue: number) {
    let params = new HttpParams();
    params = params.append('patternId', patternId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Pattern>('http://localhost:8080/api/patterns/update', {
      params: params,
    });
  }

  updateTicker(tickerId: any, updateType: number, updateValue: any) {
    let params = new HttpParams();
    params = params.append('tickerId', tickerId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get('http://localhost:8080/api/ticker/update', {
      params: params,
    });
  }


  // updateRule(playerId: number, ruleId: number, operator: number, comparison: number, newValue: number, part: number) {
  //   let params = new HttpParams();
  //   params = params.append('playerId', playerId);
  //   params = params.append('ruleId', ruleId);
  //   params = params.append('operator', operator);
  //   params = params.append('comparison', comparison);
  //   params = params.append('newValue', newValue);
  //   params = params.append('part', part);
  //   return this.http.get('http://localhost:8080/api/rule/update', {
  //     params: params,
  //   });
  // }

  updateRule(ruleId: number, updateType: number, updateValue: any) {
    let params = new HttpParams();
    params = params.append('ruleId', ruleId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Rule>('http://localhost:8080/api/rule/update', {
      params: params,
    });
  }

  refresh() {
    return this.http.get('http://localhost:8080/api/ticker/info');
  }

  addTrack(steps: Step[]) {
    return this.http.post<Step[]>('http://localhost:8080/api/sequence/play', steps);
  }

  updateStep(stepId: number, position: number, updateType: number, updateValue: number) {
    let params = new HttpParams();
    params = params.append('stepId', stepId);
    params = params.append('position', position);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Step>('http://localhost:8080/api/steps/update', {
      params: params,
    });
  }
}
