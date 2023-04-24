export class Swirl<K> {
  private items: K[];

  constructor(items: K[]) {
    this.items = items;
  }

  getItem(index: number): K {
    return this.items[index];
  }

  getItems(): K[] {
    return this.items;
  }

  forward(): void {
    const lastItem = this.items.pop();
    if (lastItem != undefined) this.items.unshift(lastItem);
  }

  reverse(): void {
    const firstItem = this.items.shift();
    if (firstItem != undefined) this.items.push(firstItem);
  }
}
