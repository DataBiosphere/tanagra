import { EntitiesApiContext, UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import { useAsyncWithApi } from "errors";
import { useAppDispatch } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import { useCallback, useContext } from "react";
import { HashRouter } from "react-router-dom";
import { AppRouter } from "router";
import * as tanagra from "tanagra-api";
import { setUnderlays } from "underlaysSlice";
import "./app.css";

enableMapSet();

// TODO(tjennison): Fetch configs from the backend.
const columns = [
  { key: "concept_name", width: "100%", title: "Concept Name" },
  { key: "concept_id", width: 120, title: "Concept ID" },
  { key: "standard_concept", width: 180, title: "Source/Standard" },
  { key: "vocabulary_id", width: 120, title: "Vocab" },
  { key: "concept_code", width: 120, title: "Code" },
];

const criteriaMenu = [
  {
    criteriaConfig: {
      type: "concept",
      name: "Conditions",
      defaultName: "Contains Conditions Codes",
      plugin: {
        columns,
        entities: [{ name: "condition", selectable: true, hierarchical: true }],
      },
    },
  },
  {
    criteriaConfig: {
      type: "concept",
      name: "Procedures",
      defaultName: "Contains Procedures Codes",
      plugin: {
        columns,
        entities: [{ name: "procedure", selectable: true, hierarchical: true }],
      },
    },
  },
  {
    criteriaConfig: {
      type: "concept",
      name: "Observations",
      defaultName: "Contains Observations Codes",
      plugin: {
        columns,
        entities: [{ name: "observation", selectable: true }],
      },
    },
  },
  {
    criteriaConfig: {
      type: "concept",
      name: "Drugs",
      defaultName: "Contains Drugs Codes",
      plugin: {
        columns,
        entities: [
          { name: "ingredient", selectable: true, hierarchical: true },
          {
            name: "brand",
            sourceConcepts: true,
            attributes: [
              "concept_name",
              "concept_id",
              "standard_concept",
              "concept_code",
            ],
            listChildren: {
              entity: "ingredient",
              idPath: "relationshipFilter.filter.binaryFilter.attributeValue",
              filter: {
                relationshipFilter: {
                  outerVariable: "ingredient",
                  newVariable: "brand",
                  newEntity: "brand",
                  filter: {
                    binaryFilter: {
                      attributeVariable: {
                        variable: "brand",
                        name: "concept_id",
                      },
                      operator: tanagra.BinaryFilterOperator.Equals,
                      attributeValue: {
                        int64Val: 0,
                      },
                    },
                  },
                },
              },
            },
          },
        ],
      },
    },
  },
  {
    title: "Demographics   ➤ ",
    subItems:[
      {
        criteriaConfig: {
          type: "attribute",
          name: "Age",
          defaultName: "Contains Age Codes",
          plugin: {
            columns,
            entities: [{ name: "age", selectable: true }],
          },
        },
      },
      {
        criteriaConfig: {
          type: "attribute",
          name: "Deceased",
          defaultName: "Contains Deceased Codes",
          plugin: {
            columns,
            entities: [{ name: "deceased", selectable: true }],
          },
        },
      },
      {
        criteriaConfig: {
          type: "attribute",
          name: "Ethnicity",
          defaultName: "Contains Ethnicity Codes",
          plugin: {
            columns,
            entities: [{ name: "ethnicity", selectable: true }],
          },
        },
      },
      {
        criteriaConfig: {
          type: "attribute",
          name: "Gender Identity",
          defaultName: "Contains Gender Identity Codes",
          plugin: {
            columns,
            entities: [{ name: "gender identity", selectable: true }],
          },
        },
      },
      {
        criteriaConfig: {
          type: "attribute",
          name: "Race",
          defaultName: "Contains Race Codes",
          plugin: {
            columns,
            entities: [{ name: "race", selectable: true }],
          },
        },
      },
      {
        criteriaConfig: {
          type: "attribute",
          name: "Sex Assigned at Birth",
          defaultName: "Contains Sex Assigned at Birth Codes",
          plugin: {
            columns,
            entities: [{ name: "sex assigned at birth", selectable: true }],
          },
        },
      },
    ],
  },
];

// Prepackaged concept sets use _ in the ids to ensure they can't conflict with
// user generated ones.
const prepackagedConceptSets = [
  {
    id: "_demographics",
    name: "Demographics",
    entity: "person",
  },
  {
    id: "_analgesics",
    name: "Analgesics",
    entity: "ingredient_occurrence",
    filter: {
      relationshipFilter: {
        outerVariable: "ingredient_occurrence",
        newVariable: "concept",
        newEntity: "ingredient",
        filter: {
          binaryFilter: {
            attributeVariable: {
              variable: "concept",
              name: "concept_id",
            },
            operator: tanagra.BinaryFilterOperator.DescendantOfInclusive,
            attributeValue: {
              int64Val: 21604253,
            },
          },
        },
      },
    },
  },
];

export default function App() {
  const dispatch = useAppDispatch();
  const underlaysApi = useContext(UnderlaysApiContext);
  const entitiesApi = useContext(EntitiesApiContext);

  const underlaysState = useAsyncWithApi(
    useCallback(async () => {
      const res = await underlaysApi.listUnderlays({});
      if (!res?.underlays || res.underlays.length == 0) {
        throw new Error("No underlays are configured.");
      }

      const entitiesResList = await Promise.all(
        res.underlays.map((u) => {
          if (!u.name) {
            throw new Error("Unnamed underlay.");
          }
          return entitiesApi.listEntities({ underlayName: u.name });
        })
      );

      dispatch(
        setUnderlays(
          entitiesResList.map((entitiesRes, i) => {
            const name = res.underlays?.[i]?.name;
            if (!name) {
              throw new Error("Unnamed underlay.");
            }
            if (!entitiesRes.entities) {
              throw new Error(`No entities in underlay ${name}`);
            }

            return {
              name,
              primaryEntity: "person",
              entities: entitiesRes.entities,
              prepackagedConceptSets,
              criteriaMenu,
            };
          })
        )
      );
    }, [])
  );

  return (
    <Loading status={underlaysState}>
      <HashRouter>
        <AppRouter />
      </HashRouter>
    </Loading>
  );
}
