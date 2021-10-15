import { v4 as uuid } from "uuid";

export class Dataset {
  constructor(public underlayName: string, public groups: Group[] = []) {}

  findGroupIndex(id: string): number {
    return this.groups.findIndex((group) => group.id === id);
  }

  findGroup(id: string): Group | undefined {
    return this.groups[this.findGroupIndex(id)];
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

  addGroupAndCriteria(kind: GroupKind, criteria: Criteria): string {
    const group = new Group(kind, [criteria]);
    this.groups.push(group);
    return group.id;
  }

  deleteCriteria(groupId: string, criteriaId: string) {
    const groupIndex = this.findGroupIndex(groupId);
    if (groupIndex === -1) {
      throw new Error("invalid group id");
    }
    const group = this.groups[groupIndex];

    const criteriaIndex = group.findCriteriaIndex(criteriaId);
    if (criteriaIndex === -1) {
      throw new Error("invalid criteria id");
    }
    group.criteria.splice(criteriaIndex, 1);

    if (group.criteria.length === 0) {
      this.groups.splice(groupIndex, 1);
    }
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

  findCriteriaIndex(id: string): number {
    return this.criteria.findIndex((criteria) => criteria.id === id);
  }

  findCriteria(id: string): Criteria | undefined {
    return this.criteria[this.findCriteriaIndex(id)];
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
