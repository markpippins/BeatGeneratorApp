export interface Listener {
  onNotify(_messageType: number, _message: string, messageValue: number): any
}
