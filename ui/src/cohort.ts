import { v4 as uuid } from "uuid";
import * as tanagra from "./tanagra-api";

export class Cohort {
  constructor(
    public name: string,
    public underlayName: string,
    public entityName: string,
    public attributes: string[] = [],
    public groups: Group[] = []
  ) {
    this.id = uuid();
  }

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

  generateQueryParameters(): tanagra.EntityDataset | null {
    const operands = this.groups
      .map((group) => group.generateFilter())
      .filter((filter) => filter) as Array<tanagra.Filter>;
    if (operands.length === 0) {
      return null;
    }

    return {
      entityVariable: "co",
      selectedAttributes: this.attributes,
      filter: {
        arrayFilter: {
          operands: operands,
          operator: tanagra.ArrayFilterOperator.And,
        },
      },
    };
  }

  id: string;
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

  generateFilter(): tanagra.Filter | null {
    const operands = this.criteria
      .map((group) => group.generateFilter())
      .filter((filter) => filter) as Array<tanagra.Filter>;
    if (operands.length === 0) {
      return null;
    }

    return {
      arrayFilter: {
        operands: operands,
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };
  }

  id: string;
  kind: GroupKind;
  criteria: Array<Criteria>;
}

export abstract class Criteria {
  constructor(public name: string) {}

  abstract renderEdit(cohort: Cohort, group: Group): JSX.Element;
  abstract renderDetails(): JSX.Element;
  abstract generateFilter(): tanagra.Filter | null;

  id = uuid();
  count = 0;
}
