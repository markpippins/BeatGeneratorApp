import { Step } from "./step"

export interface Pattern {
  id: number

  length: number

  baseNote: number

  tempo: number

  steps: Step[]
}
