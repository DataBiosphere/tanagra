import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import TextField from "@mui/material/TextField";
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
import { TreeGridData } from "components/treegrid";
import { insertConceptSet } from "conceptSetsSlice";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useAppSelector, useUnderlay } from "hooks";
import {
  ChangeEvent,
  ReactNode,
  SyntheticEvent,
  useCallback,
  useContext,
  useState,
} from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import { createUrl } from "router";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";

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

  const [dialog, showNewCohort] = useNewCohortDialog({
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
          <Paper>
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
                  <Typography variant="h6" sx={{ flexGrow: 1 }}>
                    {cohort.name}&nbsp;
                  </Typography>
                  <IconButton
                    id={`edit-cohort-${cohort.name}`}
                    color="inherit"
                    component={RouterLink}
                    to={createUrl({
                      underlayName: underlay.name,
                      cohortId: cohort.id,
                    })}
                  >
                    <EditIcon />
                  </IconButton>
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
          <Paper>
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
          <Paper>{/* TODO(tjennison): Implement attribute selection. */}</Paper>
        </Grid>
        <Grid item xs={3}>
          <Paper>
            {selectedCohorts.size > 0 && selectedConceptSets.size > 0 ? (
              <Preview
                selectedCohorts={selectedCohorts}
                selectedConceptSets={selectedConceptSets}
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

type NewCohortDialogProps = {
  callback: (name: string) => void;
};

function useNewCohortDialog(
  props: NewCohortDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => {
    setOpen(true);
  };

  const [name, setName] = useState("New Cohort");
  const onNameChange = (event: ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const onCreate = () => {
    setOpen(false);
    props.callback(name);
  };

  return [
    // eslint-disable-next-line react/jsx-key
    <Dialog
      open={open}
      onClose={() => {
        setOpen(false);
      }}
      aria-labelledby="new-cohort-dialog-title"
      maxWidth="sm"
      fullWidth
      className="new-cohort-dialog"
    >
      <DialogTitle id="new-cohort-dialog-title">New Cohort</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          id="name"
          label="Cohort Name"
          fullWidth
          variant="standard"
          value={name}
          onChange={onNameChange}
        />
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          disabled={name.length === 0}
          onClick={onCreate}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>,
    show,
  ];
}

type PreviewProps = {
  selectedCohorts: Set<string>;
  selectedConceptSets: Set<string>;
};

function Preview(props: PreviewProps) {
  const underlay = useUnderlay();
  const cohorts = useAppSelector((state) =>
    state.cohorts.filter((cohort) => props.selectedCohorts.has(cohort.id))
  );
  const workspaceConceptSets = useAppSelector((state) =>
    state.conceptSets.filter((cs) => props.selectedConceptSets.has(cs.id))
  );
  const api = useContext(EntityInstancesApiContext);

  const [tab, setTab] = useState(0);

  const tabDataState = useAsyncWithApi<PreviewTabData[]>(
    useCallback(async () => {
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
        if (props.selectedConceptSets.has(conceptSet.id)) {
          addFilter(conceptSet.entity, conceptSet.filter);
        }
      });

      workspaceConceptSets.forEach((conceptSet) => {
        const plugin = getCriteriaPlugin(conceptSet.criteria);
        if (plugin.occurrenceEntities().length != 1) {
          throw new Error("Only one entity per concept set is supported.");
        }

        const entity = plugin.occurrenceEntities()[0];
        addFilter(entity, plugin.generateFilter(entity, true));
      });

      return Promise.all(
        Array.from(entities).map(async ([entity, filters]) => {
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

          if (entity !== underlay.primaryEntity) {
            filter = {
              arrayFilter: {
                operator: tanagra.ArrayFilterOperator.And,
                operands: [
                  {
                    relationshipFilter: {
                      outerVariable: entity,
                      newVariable: underlay.primaryEntity,
                      newEntity: underlay.primaryEntity,
                      filter: filter,
                    },
                  },
                  ...(filters.length > 0
                    ? [
                        {
                          arrayFilter: {
                            operator: tanagra.ArrayFilterOperator.Or,
                            operands: filters,
                          },
                        },
                      ]
                    : []),
                ],
              },
            };
          }

          const attributes = underlay.entities
            .find((e) => e.name === entity)
            ?.attributes?.map((attribute) => attribute.name)
            .filter(
              (attribute): attribute is string =>
                !!attribute && !attribute.startsWith("t_")
            );
          if (!attributes) {
            throw new Error(`No attributes for "${entity}"`);
          }

          const entityDataset = {
            entityVariable: entity,
            selectedAttributes: attributes,
            filter: filter,
          };

          const dataParts = await Promise.all([
            (async () => {
              const res = await api.generateDatasetSqlQuery({
                entityName: entity,
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
              // TODO(tjennison): Fetch entity instances and refactor processing
              // code from criteria/concept.
              return {};
            })(),
          ]);
          return {
            entity,
            sql: dataParts[0],
            data: dataParts[1],
          };
        })
      );
    }, [props.selectedCohorts, props.selectedConceptSets])
  );

  const handleChange = (event: SyntheticEvent, newValue: number) => {
    setTab(newValue);
  };

  return (
    <>
      <Loading status={tabDataState}>
        <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
          <Tabs value={tab} onChange={handleChange}>
            {tabDataState.data?.map((data) => (
              <Tab key={data.entity} label={data.entity} />
            ))}
          </Tabs>
        </Box>
        <Typography sx={{ fontFamily: "monospace" }}>
          {tabDataState.data?.[tab]?.sql}
        </Typography>
      </Loading>
    </>
  );
}

type PreviewTabData = {
  entity: string;
  sql: string;
  data: TreeGridData;
};
