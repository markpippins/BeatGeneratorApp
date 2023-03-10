import { Instrument } from './instrument';
import { Rule } from './rule';

export interface Player {
  id: number;
  preset: number;
  instrument: Instrument;
  rules: Rule[];
  allowedControlMessages: number[];
  note: number;
  minVelocity: number;
  maxVelocity: number;
  probability: number;

}
