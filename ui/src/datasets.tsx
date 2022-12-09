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
import {
  createCriteria,
  generateCohortFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
} from "cohort";
import { insertCohort } from "cohortsSlice";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { findEntity } from "data/configuration";
import { Filter, makeArrayFilter } from "data/filter";
import { useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useAppSelector, useUnderlay } from "hooks";
import React, {
  Fragment,
  SyntheticEvent,
  useCallback,
  useMemo,
  useState,
} from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, conceptSetURL, newConceptSetURL } from "router";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";

export function Datasets() {
  const dispatch = useAppDispatch();
  const unfilteredCohorts = useAppSelector((state) => state.present.cohorts);
  const workspaceConceptSets = useAppSelector(
    (state) => state.present.conceptSets
  );
  const navigate = useNavigate();

  const underlay = useUnderlay();
  const source = useSource();

  const [selectedCohorts, updateSelectedCohorts] = useImmer(new Set<string>());
  const [selectedConceptSets, updateSelectedConceptSets] = useImmer(
    new Set<string>()
  );
  const [excludedAttributes, updateExcludedAttributes] = useImmer(
    new Map<string, Set<string>>()
  );

  const conceptSetOccurrences = useConceptSetOccurrences(selectedConceptSets);

  const [dialog, showNewCohort] = useTextInputDialog({
    title: "New cohort",
    textLabel: "Cohort name",
    buttonLabel: "Create",
    onConfirm: (name: string) => {
      const action = dispatch(insertCohort(name, underlay.name));
      navigate(cohortURL(action.payload.id, action.payload.groups[0].id));
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
            variant="body1"
            color="inherit"
            underline="hover"
            component={RouterLink}
            to={conceptSetURL(conceptSet.id)}
          >
            {conceptSet.name}
          </Link>
        ) : (
          <Typography variant="body1">{conceptSet.name}</Typography>
        )}
      </Stack>
    ));
  };

  const onInsertConceptSet = (criteria: tanagra.Criteria) => {
    navigate(newConceptSetURL(criteria.config.id));
  };

  const [menu, showInsertConceptSet] = useMenu({
    children: underlay.uiConfiguration.criteriaConfigs
      .filter((config) => !!config.conceptSet)
      .map((config) => (
        <MenuItem
          key={config.title}
          onClick={() => {
            onInsertConceptSet(createCriteria(source, config));
          }}
        >
          {config.title}
        </MenuItem>
      )),
  });

  const allAttributesChecked = () => {
    if (conceptSetOccurrences.length === 0) {
      return false;
    }

    for (const occurrence of conceptSetOccurrences) {
      for (const attribute of occurrence.attributes) {
        if (excludedAttributes.get(occurrence.id)?.has(attribute)) {
          return false;
        }
      }
    }
    return true;
  };

  const cohorts = useMemo(
    () =>
      unfilteredCohorts.filter(
        (cohort) => cohort.underlayName === underlay.name
      ),
    [unfilteredCohorts]
  );

  return (
    <>
      <ActionBar title="Datasets" />
      <Grid container columns={3} className="datasets">
        <Grid item xs={1}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
          >
            <Stack>
              <Typography variant="h4" sx={{ flexGrow: 1 }}>
                1. Select cohorts
              </Typography>
              <Typography variant="body1">
                Which participants to include
              </Typography>
            </Stack>
            <IconButton id="insert-cohort" onClick={showNewCohort}>
              <AddIcon />
            </IconButton>
            {dialog}
          </Stack>
          <Paper
            sx={{ p: 1, overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            {cohorts.length === 0 && (
              <Empty
                maxWidth="80%"
                title="No cohorts yet"
                subtitle="You can create a cohort by clicking on the '+' above"
              />
            )}
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
                    variant="body1"
                    color="inherit"
                    underline="hover"
                    component={RouterLink}
                    to={cohortURL(cohort.id, cohort.groups[0].id)}
                  >
                    {cohort.name}
                  </Link>
                </Stack>
              ))}
          </Paper>
        </Grid>
        <Grid item xs={1}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
          >
            <Stack>
              <Typography variant="h4" sx={{ flexGrow: 1 }}>
                2. Select concept sets
              </Typography>
              <Typography variant="body1">
                Which information to include about participants
              </Typography>
            </Stack>
            <IconButton id="insert-concept-set" onClick={showInsertConceptSet}>
              <AddIcon />
            </IconButton>
            {menu}
          </Stack>
          <Paper
            sx={{ p: 1, overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            {underlay.uiConfiguration.prepackagedConceptSets && (
              <>
                <Typography variant="h4">Prepackaged</Typography>
                {listConceptSets(
                  false,
                  underlay.uiConfiguration.prepackagedConceptSets
                )}
              </>
            )}
            <Typography variant="h4">Workspace</Typography>
            {listConceptSets(
              true,
              workspaceConceptSets
                .filter((cs) => cs.underlayName === underlay.name)
                .map((cs) => ({
                  id: cs.id,
                  name: getCriteriaTitle(cs.criteria),
                }))
            )}
          </Paper>
        </Grid>
        <Grid item xs={1}>
          <Stack
            direction="row"
            alignItems="center"
            justifyContent="space-between"
          >
            <Stack>
              <Typography variant="h4" mr={1}>
                3. Select values
              </Typography>
              <Typography variant="body1">
                Which columns to include in exported tables
              </Typography>
            </Stack>
            <Stack direction="row" alignItems="center">
              <Checkbox
                size="small"
                fontSize="inherit"
                name="select-all-values"
                checked={allAttributesChecked()}
                onChange={() =>
                  updateExcludedAttributes((selection) => {
                    selection.clear();
                    if (allAttributesChecked()) {
                      conceptSetOccurrences.forEach((occurrence) => {
                        selection.set(
                          occurrence.id,
                          new Set<string>(occurrence.attributes)
                        );
                      });
                    }
                  })
                }
              />
              <Typography variant="subtitle1">
                {allAttributesChecked() ? "Deselect all" : "Select all"}
              </Typography>
            </Stack>
          </Stack>
          <Paper
            sx={{ p: 1, overflowY: "auto", display: "block" }}
            className="datasets-select-panel"
          >
            {conceptSetOccurrences.length === 0 && (
              <Empty
                maxWidth="80%"
                title="No inputs selected"
                subtitle="You can view the available values by selecting at least one cohort and concept set"
              />
            )}
            {conceptSetOccurrences.map((occurrence) => (
              <Fragment key={occurrence.id}>
                <Typography variant="subtitle1">{occurrence.name}</Typography>
                {occurrence.attributes.map((attribute) => (
                  <Stack key={attribute} direction="row" alignItems="center">
                    <Checkbox
                      size="small"
                      fontSize="inherit"
                      name={occurrence.id + "-" + attribute}
                      checked={
                        !excludedAttributes.get(occurrence.id)?.has(attribute)
                      }
                      onChange={() =>
                        updateExcludedAttributes((selection) => {
                          if (!selection?.get(occurrence.id)) {
                            selection?.set(occurrence.id, new Set<string>());
                          }

                          const attributes = selection?.get(occurrence.id);
                          if (attributes?.has(attribute)) {
                            attributes?.delete(attribute);
                          } else {
                            attributes?.add(attribute);
                          }
                        })
                      }
                    />
                    <Typography variant="body1">{attribute}</Typography>
                  </Stack>
                ))}
              </Fragment>
            ))}
          </Paper>
        </Grid>
        <Grid item xs={3}>
          <Paper sx={{ p: 1 }}>
            {selectedCohorts.size > 0 && selectedConceptSets.size > 0 ? (
              <Preview
                selectedCohorts={selectedCohorts}
                selectedConceptSets={selectedConceptSets}
                conceptSetOccurrences={conceptSetOccurrences}
                excludedAttributes={excludedAttributes}
              />
            ) : (
              <Empty
                maxWidth="60%"
                minHeight="200px"
                image="/empty.png"
                title="No inputs selected"
                subtitle="You can preview the data by selecting at least one cohort and concept set"
              />
            )}
          </Paper>
        </Grid>
      </Grid>
    </>
  );
}

type ConceptSetOccurrence = {
  id: string;
  name: string;
  attributes: string[];
  filters: Filter[];
};

function useConceptSetOccurrences(
  selectedConceptSets: Set<string>
): ConceptSetOccurrence[] {
  const underlay = useUnderlay();
  const source = useSource();

  const occurrences = new Map<string, Filter[]>();
  const addFilter = (occurrence: string, filter?: Filter | null) => {
    if (!occurrences.has(occurrence)) {
      occurrences.set(occurrence, []);
    }
    if (filter) {
      occurrences.get(occurrence)?.push(filter);
    }
  };

  underlay.uiConfiguration.prepackagedConceptSets?.forEach((conceptSet) => {
    if (selectedConceptSets.has(conceptSet.id)) {
      addFilter(conceptSet.occurrence, conceptSet.filter);
    }
  });

  const workspaceConceptSets = useAppSelector((state) =>
    state.present.conceptSets.filter((cs) => selectedConceptSets.has(cs.id))
  );
  workspaceConceptSets.forEach((conceptSet) => {
    const plugin = getCriteriaPlugin(conceptSet.criteria);
    addFilter(plugin.occurrenceID(), plugin.generateFilter());
  });

  return Array.from(occurrences)
    .sort()
    .map(([id, filters]) => {
      return {
        id,
        name: findEntity(id, source.config).entity,
        attributes: source.listAttributes(id),
        filters,
      };
    });
}

type PreviewProps = {
  selectedCohorts: Set<string>;
  selectedConceptSets: Set<string>;
  conceptSetOccurrences: ConceptSetOccurrence[];
  excludedAttributes: Map<string, Set<string>>;
};

function Preview(props: PreviewProps) {
  const source = useSource();
  const cohorts = useAppSelector((state) =>
    state.present.cohorts.filter((cohort) =>
      props.selectedCohorts.has(cohort.id)
    )
  );

  const [tab, setTab] = useState(0);
  const [queriesMode, setQueriesMode] = useState(false);

  const tabDataState = useAsyncWithApi<PreviewTabData[]>(
    useCallback(async () => {
      return Promise.all(
        props.conceptSetOccurrences.map(async (occurrence) => {
          const cohortsFilter = makeArrayFilter(
            { min: 1 },
            cohorts.map((cohort) => generateCohortFilter(cohort))
          );
          if (!cohortsFilter) {
            throw new Error("All selected cohorts are empty.");
          }

          const conceptSetsFilter = makeArrayFilter(
            { min: 1 },
            occurrence.filters
          );

          const filteredAttributes = occurrence.attributes.filter(
            (a) => !props.excludedAttributes.get(occurrence.id)?.has(a)
          );

          const res = await source.listData(
            filteredAttributes,
            occurrence.id,
            cohortsFilter,
            conceptSetsFilter
          );

          const data: TreeGridData = {
            root: { data: {}, children: [] },
          };

          res.data.forEach((entry, i) => {
            data[i] = { data: entry };
            data.root?.children?.push(i);
          });

          return {
            name: occurrence.name,
            sql: res.sql,
            data: data,
          };
        })
      );
    }, [
      props.selectedCohorts,
      props.selectedConceptSets,
      props.excludedAttributes,
    ])
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
              <Tab key={data.name} label={data.name} />
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
            {tabDataState.data?.[tab]?.data?.root?.children?.length ? (
              <TreeGrid
                data={tabDataState.data?.[tab]?.data}
                columns={props.conceptSetOccurrences[tab]?.attributes
                  .filter(
                    (a) =>
                      !props.excludedAttributes
                        .get(props.conceptSetOccurrences[tab]?.id)
                        ?.has(a)
                  )
                  .map((attribute) => ({
                    key: attribute,
                    width: 120,
                    title: attribute,
                  }))}
                variableWidth
                wrapBodyText
              />
            ) : (
              <Empty
                maxWidth="60%"
                minHeight="200px"
                image="/empty.png"
                title="No data matched"
                subtitle="No data in this table matched the specified cohorts ands concept sets"
              />
            )}
          </div>
        ) : undefined}
      </Loading>
    </>
  );
}

type PreviewTabData = {
  name: string;
  sql: string;
  data: TreeGridData;
};
