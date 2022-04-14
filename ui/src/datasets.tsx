import AddIcon from "@mui/icons-material/Add";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { EntityInstancesApiContext } from "apiContext";
import {
  createCriteria,
  Criteria,
  generateQueryFilter,
  getCriteriaPlugin,
} from "cohort";
import { insertCohort } from "cohortsSlice";
import Checkbox from "components/checkbox";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData, TreeGridRowData } from "components/treegrid";
import { insertConceptSet } from "conceptSetsSlice";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useAppSelector, useUnderlay } from "hooks";
import React, {
  Fragment,
  SyntheticEvent,
  useCallback,
  useContext,
  useState,
} from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import { createUrl } from "router";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { isValid } from "util/valid";

export function Datasets() {
  const dispatch = useAppDispatch();
  const cohorts = useAppSelector((state) => state.cohorts);
  const workspaceConceptSets = useAppSelector((state) => state.conceptSets);
  const history = useHistory();

  const underlay = useUnderlay();

  const [selectedCohorts, updateSelectedCohorts] = useImmer(new Set<string>());
  const [selectedConceptSets, updateSelectedConceptSets] = useImmer(
    new Set<string>()
  );
  const [excludedAttributes, updateExcludedAttributes] = useImmer(
    new Map<string, Set<string>>()
  );

  const conceptSetEntities = useConceptSetEntities(selectedConceptSets);

  const [dialog, showNewCohort] = useTextInputDialog({
    title: "New Cohort",
    titleId: "new-cohort-dialog-title",
    textLabel: "Cohort Name",
    buttonHint: "Create",
    callback: (name: string) => {
      const action = dispatch(
        insertCohort(
          name,
          underlay.name,
          // TODO(tjennison): Populate from an actual source.
          ["person_id"]
        )
      );
      history.push(
        createUrl({
          underlayName: underlay.name,
          cohortId: action.payload.id,
        })
      );
    },
  });

  const onToggle = <T,>(
    update: (fn: (draft: Set<T>) => Set<T>) => void,
    id: T
  ) => {
    update((draft) => {
      if (draft.has(id)) {
        draft.delete(id);
      } else {
        draft.add(id);
      }
      return draft;
    });
  };

  const listConceptSets = (
    editable: boolean,
    conceptSets: { id: string; name: string }[]
  ) => {
    return conceptSets.map((conceptSet) => (
      <Stack key={conceptSet.name} direction="row" alignItems="center">
        <Checkbox
          size="small"
          fontSize="inherit"
          name={conceptSet.name}
          checked={selectedConceptSets.has(conceptSet.id)}
          onChange={() => onToggle(updateSelectedConceptSets, conceptSet.id)}
        />
        {editable ? (
          <Link
            variant="h6"
            color="inherit"
            underline="hover"
            component={RouterLink}
            to={createUrl({
              underlayName: underlay.name,
              conceptSetId: conceptSet.id,
            })}
          >
            {conceptSet.name}
          </Link>
        ) : (
          <Typography variant="h6">{conceptSet.name}</Typography>
        )}
      </Stack>
    ));
  };

  const onInsertConceptSet = (criteria: Criteria) => {
    const {
      payload: { id },
    } = dispatch(insertConceptSet(underlay.name, criteria));
    history.push(
      createUrl({
        underlayName: underlay.name,
        conceptSetId: id,
      })
    );
  };

  const [menu, showInsertConceptSet] = useMenu({
    children: underlay.criteriaConfigs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onInsertConceptSet(createCriteria(config));
        }}
      >
        {config.title}
      </MenuItem>
    )),
  });

  return (
    <>
      <ActionBar title="Datasets" />
      <Grid container columns={3} className="datasets">
        <Grid item xs={1}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="h4" sx={{ flexGrow: 1 }}>
              1. Select Cohorts
            </Typography>
            <IconButton id="insert-cohort" onClick={showNewCohort}>
              <AddIcon />
            </IconButton>
            {dialog}
          </Stack>
          <Paper
            sx={{ overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            {cohorts
              .filter((cohort) => cohort.underlayName === underlay.name)
              .map((cohort) => (
                <Stack key={cohort.id} direction="row" alignItems="center">
                  <Checkbox
                    size="small"
                    fontSize="inherit"
                    name={cohort.name}
                    checked={selectedCohorts.has(cohort.id)}
                    onChange={() => onToggle(updateSelectedCohorts, cohort.id)}
                  />
                  <Link
                    variant="h6"
                    color="inherit"
                    underline="hover"
                    component={RouterLink}
                    to={createUrl({
                      underlayName: underlay.name,
                      cohortId: cohort.id,
                    })}
                  >
                    {cohort.name}
                  </Link>
                </Stack>
              ))}
          </Paper>
        </Grid>
        <Grid item xs={1}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="h4" sx={{ flexGrow: 1 }}>
              2. Select Concept Sets
            </Typography>
            <IconButton id="insert-concept-set" onClick={showInsertConceptSet}>
              <AddIcon />
            </IconButton>
            {menu}
          </Stack>
          <Paper
            sx={{ overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            <Typography variant="h5">Prepackaged</Typography>
            {listConceptSets(false, underlay.prepackagedConceptSets)}
            <Typography variant="h5">Workspace</Typography>
            {listConceptSets(
              true,
              workspaceConceptSets
                .filter((cs) => cs.underlayName === underlay.name)
                .map((cs) => ({
                  id: cs.id,
                  name: cs.criteria.name,
                }))
            )}
          </Paper>
        </Grid>
        <Grid item xs={1}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="h4">3. Values</Typography>
            <Typography variant="h5">(Columns)</Typography>
          </Stack>
          <Paper
            sx={{ overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            {conceptSetEntities.map((entity) => (
              <Fragment key={entity.name}>
                <Typography variant="h5">{entity.name}</Typography>
                {entity.attributes.map((attribute) => (
                  <Stack key={attribute} direction="row" alignItems="center">
                    <Checkbox
                      size="small"
                      fontSize="inherit"
                      name={entity + "-" + attribute}
                      checked={
                        !excludedAttributes.get(entity.name)?.has(attribute)
                      }
                      onChange={() =>
                        updateExcludedAttributes((selection) => {
                          if (!selection?.get(entity.name)) {
                            selection?.set(entity.name, new Set<string>());
                          }

                          const attributes = selection?.get(entity.name);
                          if (attributes?.has(attribute)) {
                            attributes?.delete(attribute);
                          } else {
                            attributes?.add(attribute);
                          }
                        })
                      }
                    />
                    <Typography variant="h6">{attribute}</Typography>
                  </Stack>
                ))}
              </Fragment>
            ))}
          </Paper>
        </Grid>
        <Grid item xs={3}>
          <Paper>
            {selectedCohorts.size > 0 && selectedConceptSets.size > 0 ? (
              <Preview
                selectedCohorts={selectedCohorts}
                selectedConceptSets={selectedConceptSets}
                conceptSetEntities={conceptSetEntities}
              />
            ) : (
              <Typography variant="h5">
                Select at least one cohort and concept set to preview the
                dataset.
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </>
  );
}

type ConceptSetEntity = {
  name: string;
  attributes: string[];
  filters: tanagra.Filter[];
};

function useConceptSetEntities(
  selectedConceptSets: Set<string>
): ConceptSetEntity[] {
  const underlay = useUnderlay();

  const entities = new Map<string, tanagra.Filter[]>();
  const addFilter = (entity: string, filter?: tanagra.Filter | null) => {
    if (!entities.has(entity)) {
      entities.set(entity, []);
    }
    if (filter) {
      entities.get(entity)?.push(filter);
    }
  };

  underlay.prepackagedConceptSets.forEach((conceptSet) => {
    if (selectedConceptSets.has(conceptSet.id)) {
      addFilter(conceptSet.entity, conceptSet.filter);
    }
  });

  const workspaceConceptSets = useAppSelector((state) =>
    state.conceptSets.filter((cs) => selectedConceptSets.has(cs.id))
  );
  workspaceConceptSets.forEach((conceptSet) => {
    const plugin = getCriteriaPlugin(conceptSet.criteria);
    if (plugin.occurrenceEntities().length != 1) {
      throw new Error("Only one entity per concept set is supported.");
    }

    const entity = plugin.occurrenceEntities()[0];
    addFilter(entity, plugin.generateFilter(entity, true));
  });

  return Array.from(entities)
    .sort()
    .map(([entityName, filters]) => {
      const attributes = underlay.entities
        .find((entity) => entity.name === entityName)
        ?.attributes?.map((attribute) => attribute.name || "unknown")
        ?.filter((attribute) => !attribute.startsWith("t_"));
      return {
        name: entityName,
        attributes: attributes || [],
        filters,
      };
    });
}

type PreviewProps = {
  selectedCohorts: Set<string>;
  selectedConceptSets: Set<string>;
  conceptSetEntities: ConceptSetEntity[];
};

function Preview(props: PreviewProps) {
  const underlay = useUnderlay();
  const cohorts = useAppSelector((state) =>
    state.cohorts.filter((cohort) => props.selectedCohorts.has(cohort.id))
  );
  const api = useContext(EntityInstancesApiContext);

  const [tab, setTab] = useState(0);
  const [queriesMode, setQueriesMode] = useState(false);

  const tabDataState = useAsyncWithApi<PreviewTabData[]>(
    useCallback(async () => {
      return Promise.all(
        props.conceptSetEntities.map(async (entity) => {
          let filter: tanagra.Filter = {
            arrayFilter: {
              operands: cohorts
                .map((cohort) =>
                  generateQueryFilter(cohort, underlay.primaryEntity)
                )
                .filter((filter): filter is tanagra.Filter => !!filter),
              operator: tanagra.ArrayFilterOperator.Or,
            },
          };

          if (entity.name !== underlay.primaryEntity) {
            filter = {
              arrayFilter: {
                operator: tanagra.ArrayFilterOperator.And,
                operands: [
                  {
                    relationshipFilter: {
                      outerVariable: entity.name,
                      newVariable: underlay.primaryEntity,
                      newEntity: underlay.primaryEntity,
                      filter: filter,
                    },
                  },
                  ...(entity.filters.length > 0
                    ? [
                        {
                          arrayFilter: {
                            operator: tanagra.ArrayFilterOperator.Or,
                            operands: entity.filters,
                          },
                        },
                      ]
                    : []),
                ],
              },
            };
          }

          const entityDataset = {
            entityVariable: entity.name,
            selectedAttributes: entity.attributes,
            filter: filter,
          };

          const dataParts = await Promise.all([
            (async () => {
              const res = await api.generateDatasetSqlQuery({
                entityName: entity.name,
                underlayName: underlay.name,
                generateDatasetSqlQueryRequest: {
                  entityDataset,
                },
              });

              if (!res?.query) {
                throw new Error("Service returned an empty query.");
              }
              return res.query;
            })(),
            (async () => {
              const res = await api.searchEntityInstances({
                entityName: entity.name,
                underlayName: underlay.name,
                searchEntityInstancesRequest: {
                  entityDataset,
                },
              });

              const data: TreeGridData = {
                root: { data: {}, children: [] },
              };
              // TODO(tjennison): Use server side limits.
              res?.instances?.slice(0, 100)?.forEach((instance, i) => {
                const row: TreeGridRowData = {};
                for (const k in instance) {
                  const v = instance[k];
                  if (!v) {
                    row[k] = "";
                  } else if (isValid(v.int64Val)) {
                    row[k] = v.int64Val;
                  } else if (isValid(v.boolVal)) {
                    row[k] = v.boolVal;
                  } else {
                    row[k] = v.stringVal;
                  }
                }

                data[i] = { data: row };
                data.root?.children?.push(i);
              });
              return data;
            })(),
          ]);
          return {
            entity: entity.name,
            sql: dataParts[0],
            data: dataParts[1],
          };
        })
      );
    }, [props.selectedCohorts, props.selectedConceptSets])
  );

  const onTabChange = (event: SyntheticEvent, newValue: number) => {
    setTab(newValue);
  };

  const onQueriesModeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setQueriesMode(event.target.checked);
  };

  return (
    <>
      <Loading status={tabDataState}>
        <Stack
          direction="row"
          alignItems="center"
          sx={{ borderBottom: 1, borderColor: "divider" }}
        >
          <Tabs value={tab} onChange={onTabChange} sx={{ flexGrow: 1 }}>
            {tabDataState.data?.map((data) => (
              <Tab key={data.entity} label={data.entity} />
            ))}
          </Tabs>
          <Typography variant="button">Data</Typography>
          <Switch onChange={onQueriesModeChange} name="queries-mode" />
          <Typography variant="button">Queries</Typography>
        </Stack>
        {queriesMode ? (
          <Typography sx={{ fontFamily: "monospace" }}>
            {tabDataState.data?.[tab]?.sql}
          </Typography>
        ) : tabDataState.data?.[tab]?.data ? (
          <div
            style={{
              overflowX: "auto",
              display: "block",
            }}
          >
            <TreeGrid
              data={tabDataState.data?.[tab]?.data}
              columns={props.conceptSetEntities[tab]?.attributes.map(
                (attribute) => ({
                  key: attribute,
                  width: 120,
                  title: attribute,
                })
              )}
              variableWidth
              wrapBodyText
            />
          </div>
        ) : undefined}
      </Loading>
    </>
  );
}

type PreviewTabData = {
  entity: string;
  sql: string;
  data: TreeGridData;
};
