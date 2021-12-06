import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EntityInstancesApiContext } from "apiContext";
import { Cohort, Group, GroupKind } from "cohort";
import { CohortUpdaterProvider } from "cohortUpdaterContext";
import { ConceptCriteria } from "criteria/concept";
import * as tanagra from "./tanagra-api";

test.each([
  [
    "source",
    {
      instance: {
        concept_name: {
          stringVal: "test-concept",
        },
        concept_id: {
          int64Val: 1234,
        },
        domain_id: {
          int64Val: 5678,
        },
      },
      matches: ["test-concept", "1234", "5678", "Source"],
    },
  ],
  [
    "standard",
    {
      instance: {
        concept_name: {
          stringVal: "test-concept",
        },
        concept_id: {
          int64Val: 1234,
        },
        standard_concept: {
          stringVal: "S",
        },
      },
      matches: ["test-concept", "1234", "Standard"],
    },
  ],
  [
    "missing concept_id",
    {
      instance: {
        concept_name: {
          stringVal: "test-concept",
        },
      },
      matches: ["0-0 of 0"],
    },
  ],
])("%s", async (name, { instance, matches }) => {
  await renderCriteria([instance]);

  matches.forEach((match) => screen.getByText(match));
});

test("selection", async () => {
  const { getCriteria, rerender } = await renderCriteria([
    {
      concept_id: {
        int64Val: 100,
      },
    },
    {
      concept_id: {
        int64Val: 101,
      },
    },
  ]);

  const checkboxes = screen.getAllByRole("checkbox");
  expect(checkboxes.length).toBe(2);

  expect(getCriteria().selected.length).toBe(0);

  const getSelected = () => getCriteria().selected.map((row) => row.id);

  // Use act explicitly because DataGrid has a focus update that occurs after
  // the event (i.e. outside of act) which causes a warning.
  act(() => userEvent.click(checkboxes[0]));
  expect(getSelected()).toEqual([100]);

  rerender();

  act(() => {
    userEvent.click(checkboxes[1]);
  });
  expect(getSelected()).toEqual([100, 101]);

  rerender();

  act(() => {
    userEvent.click(checkboxes[0]);
  });
  expect(getSelected()).toEqual([101]);
});

async function renderCriteria(
  instances: Array<{ [key: string]: tanagra.AttributeValue }>
) {
  const criteria = new ConceptCriteria("test-criteria", "test-filter");
  const group = new Group(GroupKind.Included, [criteria]);
  let cohort = new Cohort(
    "test-cohort",
    "test-underlay",
    "test-entity",
    [],
    [group]
  );

  const getCohort = () => cohort;
  const getCriteria = () => getCohort().groups[0].criteria[0];
  const setCohort = (c: Cohort) => {
    cohort = c;
  };

  const api = {
    async searchEntityInstances(): Promise<tanagra.SearchEntityInstancesResponse> {
      return new Promise<tanagra.SearchEntityInstancesResponse>((resolve) => {
        resolve({
          instances: instances,
        });
      });
    },
  };

  const components = () => (
    <CohortUpdaterProvider cohort={getCohort()} setCohort={setCohort}>
      <EntityInstancesApiContext.Provider value={api}>
        {getCriteria().renderEdit(getCohort(), group)}
      </EntityInstancesApiContext.Provider>
    </CohortUpdaterProvider>
  );

  const { rerender } = render(components());

  await screen.findByText("Concept Name");
  return {
    getCriteria: getCriteria,
    rerender: () => rerender(components()),
  };
}
