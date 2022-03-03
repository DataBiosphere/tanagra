import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { EntityInstancesApiContext } from "apiContext";
import { insertCohort } from "cohortsSlice";
import Loading from "components/loading";
import { TreeGridData } from "components/treegrid";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useAppSelector, useUnderlayOrFail } from "hooks";
import {
  ChangeEvent,
  ReactNode,
  SyntheticEvent,
  useCallback,
  useContext,
  useState,
} from "react";
import { Link as RouterLink } from "react-router-dom";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { generateQueryFilter } from "./cohort";

export function Datasets() {
  const dispatch = useAppDispatch();
  const cohorts = useAppSelector((state) => state.cohorts);

  const underlay = useUnderlayOrFail();

  const [selectedCohorts, updateSelectedCohorts] = useImmer(new Set<string>());
  const [selectedConceptSets, updateSelectedConceptSets] = useImmer(
    new Set<string>()
  );

  const [dialog, show] = useNewCohortDialog({
    callback: (name: string) => {
      dispatch(
        insertCohort(
          name,
          underlay.name,
          // TODO(tjennison): Populate from an actual source.
          ["person_id"]
        )
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

  return (
    <>
      <ActionBar title="Datasets" backUrl="/" />
      <Grid container columns={3} className="datasets">
        <Grid item xs={1}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="h4" sx={{ flexGrow: 1 }}>
              1. Select Cohorts
            </Typography>
            <IconButton id="insert-cohort" onClick={show}>
              <AddIcon />
            </IconButton>
            {dialog}
          </Stack>
          <Paper>
            {cohorts
              .filter((cohort) => cohort.underlayName === underlay.name)
              .map((cohort) => (
                <Stack key={cohort.id} direction="row" alignItems="baseline">
                  <Checkbox
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
                    to={`/${underlay.name}/${cohort.id}`}
                  >
                    <EditIcon />
                  </IconButton>
                </Stack>
              ))}
          </Paper>
        </Grid>
        <Grid item xs={1}>
          <Typography variant="h4">2. Select Concept Sets</Typography>
          <Paper>
            <Typography variant="h5">Prepackaged</Typography>
            {underlay.prepackagedConceptSets.map((conceptSet) => (
              <Stack
                key={conceptSet.name}
                direction="row"
                alignItems="baseline"
              >
                <Checkbox
                  name={conceptSet.name}
                  checked={selectedConceptSets.has(conceptSet.id)}
                  onChange={() =>
                    onToggle(updateSelectedConceptSets, conceptSet.id)
                  }
                />
                <Typography variant="h6">{conceptSet.name}</Typography>
              </Stack>
            ))}
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
  const underlay = useUnderlayOrFail();
  const cohorts = useAppSelector((state) =>
    state.cohorts.filter((cohort) => props.selectedCohorts.has(cohort.id))
  );
  const api = useContext(EntityInstancesApiContext);

  const [tab, setTab] = useState(0);

  const tabDataState = useAsyncWithApi<PreviewTabData[]>(
    useCallback(async () => {
      const entities = new Map<string, tanagra.Filter[]>();
      underlay.prepackagedConceptSets.forEach((conceptSet) => {
        if (props.selectedConceptSets.has(conceptSet.id)) {
          if (!entities.has(conceptSet.entity)) {
            entities.set(conceptSet.entity, []);
          }
          if (conceptSet.filter) {
            entities.get(conceptSet.entity)?.push(conceptSet.filter);
          }
        }
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
