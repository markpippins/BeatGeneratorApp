export interface Listener {
  notify(messageType: number, message: string): any
}
