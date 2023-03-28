import { Step } from "./step"

export interface Pattern {
  id: number

  instrumentId: number

  songId: number

  length: number

  baseNote: number

  tempo: number

  position: number

  channel: number

  scale: number

  active: boolean

  probability: number

  direction: number

  lastStep: number

  random: number

  transpose: number

  oneShot: boolean;

  loops: number;

  swing: number;

  gate: number;

  delay: number;

  steps: Step[]
}
