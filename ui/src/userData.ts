import { Cohort } from "cohort";

export class UserData {
  constructor(public entityName: string, public cohorts: Cohort[] = []) {}

  findCohortIndex(id: string): number {
    return this.cohorts.findIndex((cohort) => cohort.id === id);
  }

  findCohort(id: string): Cohort | undefined {
    return this.cohorts[this.findCohortIndex(id)];
  }
}
