export interface Condition extends Iterable<any> {
  id: string;
  operator: string;
  comparison: string;
  value: number;
}
