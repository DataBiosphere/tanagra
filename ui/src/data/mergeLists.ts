import { SortDirection } from "data/configuration";

export function mergeLists<T, R>(
  lists: [string, T[]][],
  maxCount: number,
  direction: SortDirection,
  get: (value: T) => R
): MergedItem<T>[] {
  const merged: MergedItem<T>[] = [];
  const sources = lists.map(
    ([source, data]) => new MergeSource<T>(source, data)
  );
  while (true) {
    let maxSource: MergeSource<T> | undefined;

    sources.forEach((source) => {
      if (!source.done()) {
        if (!maxSource) {
          maxSource = source;
        } else {
          const value = get(source.peek());
          const maxValue = get(maxSource.peek());
          if (
            (value && !maxValue) ||
            (value &&
              maxValue &&
              (direction === SortDirection.Asc
                ? value < maxValue
                : value > maxValue))
          ) {
            maxSource = source;
          }
        }
      }
    });
    if (!maxSource || merged.length === maxCount) {
      break;
    }
    merged.push({ source: maxSource.source, data: maxSource.pop() });
  }

  return merged;
}

export type MergedItem<T> = {
  source: string;
  data: T;
};

class MergeSource<T> {
  constructor(public source: string, private data: T[]) {
    this.source = source;
    this.data = data;
    this.current = 0;
  }

  done() {
    return this.current === this.data.length;
  }

  peek() {
    return this.data[this.current];
  }

  pop(): T {
    const data = this.peek();
    this.current++;
    return data;
  }

  private current: number;
}
