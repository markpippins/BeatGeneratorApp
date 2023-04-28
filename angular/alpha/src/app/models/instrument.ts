import {ControlCode} from "./control-code"

export interface Instrument {
  id: number
  name: string
  deviceName: string
  channel: number
  lowestNote: number
  highestNote: number
  highestPreset: number
  preferredPreset: number
  assignments: Map<number, string>
  boundaries: Map<number, number[]>
  hasAssignments: boolean
  pads: number
  controlCodes: ControlCode[]
}
