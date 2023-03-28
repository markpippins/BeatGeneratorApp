export interface Step {
  id: number
  channel: number
  position: number
  page: number
  songId: number
  active: boolean
  pitch: number
  velocity: number
  probability: number
  gate: number
}
