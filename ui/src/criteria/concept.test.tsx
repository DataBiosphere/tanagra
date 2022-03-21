import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EntityInstancesApiContext } from "apiContext";
import { createCriteria, getCriteriaPlugin, GroupKind } from "cohort";
import { insertCohort, insertGroup } from "cohortsSlice";
import "criteria/concept";
import { Provider } from "react-redux";
import { StaticRouter } from "react-router-dom";
import { AppRouter } from "router";
import { store } from "store";
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
        standard_concept: null,
      },
      matches: ["test-concept", "1234", "Source"],
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
      notMatches: ["test-concept"],
    },
  ],
])("%s", async (name, { instance, matches, notMatches }) => {
  await renderCriteria([instance]);

  matches?.forEach((match) => screen.getByText(match));
  notMatches?.forEach((match) => {
    expect(screen.queryByText(match)).not.toBeInTheDocument();
  });
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

  expect(getCriteria().data.selected.length).toBe(0);

  const getSelected = () => getCriteria().data.selected.map((row) => row.id);

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

beforeAll(() => {
  const action = store.dispatch(
    insertCohort("test-cohort", "test-underlay", "test-entity", [])
  );
  store.dispatch(
    insertGroup(
      action.payload.id,
      GroupKind.Included,
      createCriteria({
        type: "concept",
        title: "Conditions",
        defaultName: "Contains Conditions Codes",
        plugin: {
          columns: [
            { key: "concept_name", width: "100%", title: "Concept Name" },
            { key: "concept_id", width: 120, title: "Concept ID" },
            { key: "standard_concept", width: 180, title: "Source/Standard" },
            { key: "vocabulary_id", width: 120, title: "Vocab" },
            { key: "concept_code", width: 120, title: "Code" },
          ],
          entities: [
            { name: "condition", selectable: true, hierarchical: true },
          ],
        },
      })
    )
  );
});

async function renderCriteria(
  instances: Array<{ [key: string]: tanagra.AttributeValue }>
) {
  const getCriteria = () => store.getState().cohorts[0].groups[0].criteria[0];

  const api = {
    async searchEntityInstances(): Promise<tanagra.SearchEntityInstancesResponse> {
      return new Promise<tanagra.SearchEntityInstancesResponse>((resolve) => {
        resolve({
          instances: instances,
        });
      });
    },
  };

  const cohort = store.getState().cohorts[0];
  const group = cohort.groups[0];
  const criteria = group.criteria[0];

  const components = () => (
    <Provider store={store}>
      <EntityInstancesApiContext.Provider value={api}>
        <StaticRouter
          location={`/test-underlay/cohorts/${cohort.id}/edit/${group.id}/${criteria.id}`}
        >
          <AppRouter>
            {getCriteriaPlugin(getCriteria()).renderEdit(jest.fn())}
          </AppRouter>
        </StaticRouter>
      </EntityInstancesApiContext.Provider>
    </Provider>
  );

  const { rerender } = render(components());

  await screen.findByText("Concept Name");
  return {
    getCriteria: getCriteria,
    rerender: () => rerender(components()),
  };
}
