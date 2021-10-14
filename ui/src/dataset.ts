import { v4 as uuid } from "uuid";

export class Dataset {
  constructor(public underlayName: string, public groups: Group[] = []) {}

  findGroup(id: string): Group | undefined {
    return this.groups.find((group) => group.id === id);
  }

  listGroups(kind: GroupKind): Group[] {
    return this.groups.filter((group) => group.kind == kind);
  }

  addCriteria(groupId: string, criteria: Criteria) {
    const group = this.findGroup(groupId);
    if (!group) {
      throw new Error("invalid group id");
    }
    group.criteria.push(criteria);
  }

  addGroupAndCriteria(kind: GroupKind, criteria: Criteria) {
    this.groups.push(new Group(kind, [criteria]));
  }
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
  constructor(public name: string) {}

  abstract renderEdit(dataset: Dataset, group: Group): JSX.Element;
  abstract renderDetails(): JSX.Element;

  id = uuid();
  count = 0;
}
