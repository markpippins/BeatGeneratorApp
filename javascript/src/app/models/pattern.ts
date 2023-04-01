import { Step } from "./step"

export interface Pattern {
  id: number

  name: string

  instrumentId: number

  preset: number

  songId: number

  length: number

  rootNote: number

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

  repeats: number;

  swing: number;

  gate: number;

  delay: number;

  steps: Step[]
}
