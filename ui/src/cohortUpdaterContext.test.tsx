import { Cohort, GroupKind } from "cohort";
import { ConceptCriteria } from "criteria/concept";
import { CohortUpdater } from "./cohortUpdaterContext";

test("updates", () => {
  let cohort = new Cohort("name", "underlay", "entity");
  const setCohort = (c: Cohort) => {
    cohort = c;
  };

  expect(cohort.groups.length).toBe(0);

  let prevCohort = cohort;
  new CohortUpdater(cohort, setCohort).update((c: Cohort) => {
    c.addGroupAndCriteria(
      GroupKind.Included,
      new ConceptCriteria("test-name", "test-filter")
    );
  });

  expect(cohort).not.toBe(prevCohort);
  expect(cohort.groups).toEqual([
    expect.objectContaining({
      criteria: [expect.objectContaining({ name: "test-name" })],
    }),
  ]);

  const group = cohort.groups[0];
  new CohortUpdater(cohort, setCohort).updateCriteria(
    group.id,
    group.criteria[0].id,
    (criteria: ConceptCriteria) => {
      criteria.name = "new-test-name";
    }
  );

  expect(cohort.groups).toEqual([
    expect.objectContaining({
      criteria: [expect.objectContaining({ name: "new-test-name" })],
    }),
  ]);
});
