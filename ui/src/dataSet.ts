import { v4 as uuid } from "uuid";

export class DataSet {
  constructor(underlayName: string, groups: Group[] = []) {
    this.underlayName = underlayName;
    this.groups = groups;
  }

  findGroup(id: string): Group | undefined {
    return this.groups.find((group) => group.id === id);
  }

  listGroups(kind: GroupKind): Group[] {
    const groups: Group[] = [];
    this.groups.forEach((group) => {
      if (group.kind === kind) {
        groups.push(group);
      }
    });
    return groups;
  }

  underlayName: string;
  groups: Array<Group>;
}

export enum GroupKind {
  Included = 1,
  Excluded,
}

export class Group {
  constructor(kind: GroupKind, criteria: Criteria[] = []) {
    this.id = uuid();
    this.kind = kind;
    this.criteria = criteria;
  }

  findCriteria(id: string): Criteria | undefined {
    return this.criteria.find((criteria) => criteria.id === id);
  }

  id: string;
  kind: GroupKind;
  criteria: Array<Criteria>;
}

export abstract class Criteria {
  abstract edit(dataSet: DataSet, group: Group): JSX.Element;
  abstract details(): JSX.Element;

  id = uuid();
  name = "";
  count = 0;
}
