import { Instrument } from './instrument';
import { Rule } from './rule';

export interface Player {
  id: number;
  preset: number;
  instrumentId: number;
  channel: number;
  rules: Rule[];
  allowedControlMessages: number[];
  note: number;
  minVelocity: number;
  maxVelocity: number;
  probability: number;

}
