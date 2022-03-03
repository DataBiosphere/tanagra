import { EntitiesApiContext, UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useCohort, useUnderlay } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import { useCallback, useContext } from "react";
import {
  HashRouter,
  Redirect,
  Route,
  Switch,
  useRouteMatch,
} from "react-router-dom";
import * as tanagra from "tanagra-api";
import { UnderlaySelect } from "underlaySelect";
import { setUnderlays } from "underlaysSlice";
import "./app.css";
import { Datasets } from "./datasets";
import Edit from "./edit";
import Overview from "./overview";

enableMapSet();

type AppProps = {
  entityName: string;
};

// TODO(tjennison): Fetch configs from the backend.
const columns = [
  { key: "concept_name", width: "100%", title: "Concept Name" },
  { key: "concept_id", width: 120, title: "Concept ID" },
  { key: "standard_concept", width: 180, title: "Source/Standard" },
  { key: "vocabulary_id", width: 120, title: "Vocab" },
  { key: "concept_code", width: 120, title: "Code" },
];

const criteriaConfigs = [
  {
    type: "concept",
    title: "Conditions",
    defaultName: "Contains Conditions Codes",
    plugin: {
      columns,
      entities: [{ name: "condition", selectable: true, hierarchical: true }],
    },
  },
  {
    type: "concept",
    title: "Procedures",
    defaultName: "Contains Procedures Codes",
    plugin: {
      columns,
      entities: [{ name: "procedure", selectable: true, hierarchical: true }],
    },
  },
  {
    type: "concept",
    title: "Observations",
    defaultName: "Contains Observations Codes",
    plugin: {
      columns,
      entities: [{ name: "observation", selectable: true }],
    },
  },
  {
    type: "concept",
    title: "Drugs",
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
];

export default function App(props: AppProps) {
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
              entities: entitiesRes.entities,
              criteriaConfigs,
            };
          })
        )
      );
    }, [])
  );

  return (
    <Loading status={underlaysState}>
      <HashRouter basename="/">
        <Switch>
          <Route exact path="/">
            <UnderlaySelect />
          </Route>
          <Route path="/:underlayName?/:cohortId?">
            <CohortRouter entityName={props.entityName} />
          </Route>
        </Switch>
      </HashRouter>
    </Loading>
  );
}

function CohortRouter(props: { entityName: string }) {
  const match = useRouteMatch();
  const underlay = useUnderlay();
  const cohort = useCohort();

  if (underlay && cohort) {
    return (
      <Switch>
        <Route path={`${match.path}/edit/:group/:criteria`}>
          <Edit cohort={cohort} />
        </Route>
        <Route path={match.path}>
          <Overview cohort={cohort} />
        </Route>
      </Switch>
    );
  }
  if (underlay) {
    return (
      <>
        <Redirect to={`/${underlay.name}`} />
        <Datasets entityName={props.entityName} />
      </>
    );
  }
  return <Redirect to="/" />;
}
