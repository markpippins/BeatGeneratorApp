import { Instrument } from './instrument';
import { Rule } from './rule';

export interface Player {
  id: number
  tickerId: number
  instrumentId: number
  part: number
  preset: number
  rules: Rule[]
  allowedControlMessages: number[]
  parts: number[]
  note: number
  minVelocity: number
  maxVelocity: number
  probability: number
  muted: boolean
  name: string
}
