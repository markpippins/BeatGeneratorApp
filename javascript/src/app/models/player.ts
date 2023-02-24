import { Condition } from './condition';

export interface Player {
  id: number;
  preset: number;
  instrument: string;
  channel: number;
  conditions: Condition[];
  allowedControlMessages: number[];
  note: number;
  minVelocity: number;
  maxVelocity: number;
}
