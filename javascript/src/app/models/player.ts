import { Rule } from './rule';

export interface Player {
  id: number;
  preset: number;
  instrument: string;
  channel: number;
  rules: Rule[];
  allowedControlMessages: number[];
  note: number;
  minVelocity: number;
  maxVelocity: number;
  probability: number;

}
