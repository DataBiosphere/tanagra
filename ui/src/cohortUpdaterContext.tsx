import * as _ from "lodash";
import { createContext, ReactNode, useContext, useMemo } from "react";
import { Cohort, Criteria } from "./cohort";

const CohortUpdaterContext = createContext<CohortUpdater | null>(null);

export function useCohortUpdater(): CohortUpdater {
  const updater = useContext(CohortUpdaterContext);
  if (!updater) {
    throw new Error("invalid updater context");
  }
  return updater;
}

class CohortUpdater {
  constructor(
    public cohort: Cohort,
    public setCohort: (cohort: Cohort) => void
  ) {}

  update(callback: (cohort: Cohort) => void): void {
    const newCohort = _.cloneDeep(this.cohort);
    callback(newCohort);
    this.setCohort(newCohort);
  }

  updateCriteria<Type extends Criteria>(
    groupId: string,
    criteriaId: string,
    callback: (criteria: Type) => void
  ): void {
    this.update((cohort: Cohort) => {
      const group = cohort.findGroup(groupId);
      const criteria = group?.findCriteria(criteriaId);
      if (!criteria) {
        throw new Error("criteria not found");
      }
      callback(criteria as Type);
    });
  }
}

type CohortUpdaterProviderProps = {
  cohort: Cohort;
  setCohort(cohort: Cohort): void;
  children?: ReactNode;
};

export function CohortUpdaterProvider(props: CohortUpdaterProviderProps) {
  const updater = useMemo(
    () => new CohortUpdater(props.cohort, props.setCohort),
    [props.cohort, props.setCohort]
  );

  return (
    <CohortUpdaterContext.Provider value={updater}>
      {props.children}
    </CohortUpdaterContext.Provider>
  );
}
