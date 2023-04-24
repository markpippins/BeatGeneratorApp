export class Swirl {
  private strings: string[];

  constructor(strings: string[]) {
    this.strings = strings;
  }

  forward(): void {
    const lastString = this.strings.pop();
    if (lastString != undefined) this.strings.unshift(lastString);
  }

  reverse(): void {
    const firstString = this.strings.shift();
    if (firstString != undefined) this.strings.push(firstString);
  }
}
