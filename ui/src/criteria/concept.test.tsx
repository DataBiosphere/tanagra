import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EntityInstancesApiContext } from "apiContext";
import { createCriteria } from "cohort";
import { insertCohort, insertGroup } from "cohortsSlice";
import "criteria/concept";
import { BackendSource } from "data/source";
import { Provider } from "react-redux";
import { StaticRouter } from "react-router-dom";
import { AppRouter } from "router";
import { createStore } from "store";
import * as tanagra from "tanagra-api";
import { setUnderlays } from "underlaysSlice";
import { Data } from "./concept";

// The Typescript compiler can't map the different parameters of the test cases
// to a single type without actually having a type defined.
type TestCase = {
  name: string;
  instance: { [key: string]: tanagra.AttributeValue | null };
  matches?: string[];
  notMatches?: string[];
};

const testCases: TestCase[] = [
  {
    name: "source",
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
  {
    name: "standard",
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
];

test.each(testCases)(
  "$name",
  async ({ instance, matches = [], notMatches = [] }) => {
    await renderCriteria([instance]);

    matches?.forEach((match) => screen.getByText(match));
    notMatches?.forEach((match) => {
      expect(screen.queryByText(match)).not.toBeInTheDocument();
    });
  }
);

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

  const getData = () => getCriteria().data as Data;
  expect(getData().selected.length).toBe(0);

  const getSelected = () => getData().selected.map((row) => row.key);

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
  instances: Array<{ [key: string]: tanagra.AttributeValue | null }>
) {
  const api = {
    async searchEntityInstances(): Promise<tanagra.SearchEntityInstancesResponse> {
      return new Promise<tanagra.SearchEntityInstancesResponse>((resolve) => {
        resolve({
          instances: instances,
        });
      });
    },
  } as unknown as tanagra.EntityInstancesApi;

  const dataConfig = {
    primaryEntity: {
      entity: "person",
      key: "person_id",
    },
    occurrences: [
      {
        id: "condition_occurrence",
        entity: "condition_occurrence",
        key: "concept_id",
        classifications: [
          {
            id: "condition",
            attribute: "condition_concept_id",
            entity: "condition",
            entityAttribute: "concept_id",
            hierarchical: true,
          },
        ],
      },
    ],
  };

  const demographicChartConfigs = {
    additionalSelectedAttributes: ["gender", "race"],
    groupByAttributes: [
      "gender_concept_id",
      "race_concept_id",
      "year_of_birth",
    ],
    chartConfigs: [
      {
        title: "Graph 1",
        primaryProperties: [{ key: "gender" }],
      },
    ],
  };

  const underlay = {
    name: "test-underlay",
    primaryEntity: "test-entity",
    entities: [],
    uiConfiguration: {
      dataConfig: dataConfig,
      criteriaConfigs: [],
      demographicChartConfigs: demographicChartConfigs,
    },
    prepackagedConceptSets: [],
  };

  const source = new BackendSource(
    api,
    {} as unknown as tanagra.EntityCountsApi,
    underlay,
    underlay.uiConfiguration.dataConfig
  );

  const store = createStore();

  store.dispatch(setUnderlays([underlay]));

  const action = store.dispatch(insertCohort("test-cohort", "test-underlay"));
  store.dispatch(
    insertGroup(
      action.payload.id,
      tanagra.GroupKindEnum.Included,
      createCriteria(source, {
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
          occurrence: "condition_occurrence",
          classification: "condition",
        },
      })
    )
  );

  const getCriteria = () =>
    store.getState().present.cohorts[0].groups[0].criteria[0];

  const cohort = store.getState().present.cohorts[0];
  const group = cohort.groups[0];
  const criteria = group.criteria[0];

  const components = () => (
    <Provider store={store}>
      <EntityInstancesApiContext.Provider value={api}>
        <StaticRouter
          location={`/test-underlay/cohorts/${cohort.id}/edit/${group.id}/${criteria.id}`}
        >
          <AppRouter />
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
