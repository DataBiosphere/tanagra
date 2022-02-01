import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EntityInstancesApiContext } from "apiContext";
import { createCriteria, getCriteriaPlugin, GroupKind } from "cohort";
import { insertCohort, insertGroup } from "cohortsSlice";
import "criteria/concept";
import { Provider } from "react-redux";
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

async function renderCriteria(
  instances: Array<{ [key: string]: tanagra.AttributeValue }>
) {
  const action = store.dispatch(
    insertCohort("test-cohort", "test-underlay", "test-entity", [])
  );
  store.dispatch(
    insertGroup(
      action.payload.id,
      GroupKind.Included,
      createCriteria("condition")
    )
  );

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

  const components = () => (
    <Provider store={store}>
      <EntityInstancesApiContext.Provider value={api}>
        {getCriteriaPlugin(getCriteria()).renderEdit(
          store.getState().cohorts[0],
          store.getState().cohorts[0].groups[0]
        )}
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
