export interface Listener {
  onNotify(messageType: number, message: string): any
}
