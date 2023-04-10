import { Step } from "./step"

export interface Pattern {
  id: number

  name: string

  instrumentId: number

  preset: number

  songId: number

  length: number

  speed: number

  rootNote: number

  tempo: number

  position: number

  channel: number

  active: boolean

  quantize: boolean

  scale: string

  direction: number

  lastStep: number

  random: number

  transpose: number

  repeats: number;

  swing: number;

  gate: number;

  delay: number;

  steps: Step[]
}
