export interface Listener {
  onNotify(messageType: number, message: string, messageValue: number): any
}
