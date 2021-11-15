import SchemaIcon from "@mui/icons-material/Schema";
import Checkbox from "@mui/material/Checkbox";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  DataGrid,
  GridColDef,
  GridRenderCellParams,
  GridRowData,
  GridValueFormatterParams,
} from "@mui/x-data-grid";
import { EntityInstancesApiContext } from "apiContext";
import { Cohort, Criteria, Group } from "cohort";
import { useCohortUpdater } from "cohortUpdaterContext";
import { useAsyncWithApi } from "errors";
import Loading from "loading";
import React, { useCallback, useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";

export class ConceptCriteria extends Criteria {
  constructor(name: string, public filter: string) {
    super(name);
  }

  renderEdit(cohort: Cohort, group: Group): JSX.Element {
    return (
      <ConceptEdit
        cohort={cohort}
        group={group}
        criteria={this}
        filter={this.filter}
      />
    );
  }

  renderDetails(): JSX.Element {
    return <ConceptDetails criteria={this} />;
  }

  generateFilter(): tanagra.Filter | null {
    if (this.selected.length === 0) {
      return null;
    }

    const operands = this.selected.map((row) => {
      return {
        binaryFilter: {
          attributeVariable: {
            variable: "co",
            name: "condition_concept_id",
          },
          operator: tanagra.BinaryFilterOperator.DescendantOfInclusive,
          attributeValue: {
            int64Val: row.id,
          },
        },
      };
    });

    return {
      arrayFilter: {
        operands,
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };
  }

  selected = new Array<GridRowData>();
}

const fetchedColumns: GridColDef[] = [
  { field: "concept_name", headerName: "Concept Name", width: 400 },
  { field: "concept_id", headerName: "Concept ID", width: 100 },
  { field: "domain_id", headerName: "Domain", width: 100 },
  {
    field: "standard_concept",
    headerName: "Source/Standard",
    width: 140,
    valueFormatter: (params: GridValueFormatterParams) =>
      !params.value ? "Source" : "Standard",
  },
  { field: "vocabulary_id", headerName: "Vocab", width: 120 },
  { field: "concept_code", headerName: "Code", width: 120 },
  // TODO(tjennison): Add support for counts to the API.
  //{ field: "roll_up_count", headerName: "Roll-up Count", width: 100 },
  //{ field: "item_count", headerName: "Item Count", width: 100 },
];

type ConceptEditProps = {
  cohort: Cohort;
  group: Group;
  criteria: ConceptCriteria;
  filter: string;
};

function ConceptEdit(props: ConceptEditProps) {
  const api = useContext(EntityInstancesApiContext);

  const conceptsState = useAsyncWithApi<Array<GridRowData>>(
    useCallback(
      () =>
        api
          .searchEntityInstances({
            entityName: "concept",
            underlayName: props.cohort.underlayName,
            searchEntityInstancesRequest: {
              entityDataset: {
                entityVariable: "c",
                selectedAttributes: fetchedColumns.map((col) => {
                  return col.field;
                }),
                filter: {
                  relationshipFilter: {
                    outerVariable: "c",
                    newVariable: "cc",
                    newEntity: props.filter,
                  },
                },
              },
            },
          })
          .then((res) => {
            if (res.instances) {
              return res.instances
                .map((instance) => {
                  const id = instance["concept_id"]?.int64Val || 0;
                  const row: GridRowData = {
                    id: id,
                    SELECTED:
                      props.criteria.selected.findIndex(
                        (row) => row.id === id
                      ) > -1,
                  };
                  for (const k in instance) {
                    const v = instance[k];
                    if (!v) {
                      row[k] = "";
                    } else if (v.int64Val !== null) {
                      row[k] = v.int64Val;
                    } else if (v.boolVal !== null) {
                      row[k] = v.boolVal;
                    } else {
                      row[k] = v.stringVal;
                    }
                  }
                  return row;
                })
                .filter((row) => row.id !== 0);
            }
            return [];
          }),
      [api]
    )
  );

  const updater = useCohortUpdater();

  const renderSelected = useCallback(
    (params: GridRenderCellParams) => {
      const index = props.criteria.selected.findIndex(
        (row) => row.id === params.row.id
      );
      return (
        <Stack direction="row">
          <Checkbox
            checked={index > -1}
            inputProps={{ "aria-label": "controlled" }}
            onChange={() => {
              updater.updateCriteria(
                props.group.id,
                props.criteria.id,
                (criteria: ConceptCriteria) => {
                  if (index > -1) {
                    criteria.selected.splice(index, 1);
                  } else {
                    criteria.selected.push(params.row);
                  }
                }
              );
            }}
          />
          <IconButton>
            <SchemaIcon />
          </IconButton>
        </Stack>
      );
    },
    [updater]
  );

  const columns: GridColDef[] = useMemo(
    () => [
      {
        field: "SELECTED",
        headerName: "✓",
        type: "boolean",
        width: 80,
        renderCell: renderSelected,
      },
      ...fetchedColumns,
    ],
    [renderSelected]
  );

  return (
    <Loading status={conceptsState}>
      {conceptsState.data ? (
        <DataGrid
          autoHeight
          rows={conceptsState.data}
          columns={columns}
          disableSelectionOnClick
          className="criteria-concept"
        />
      ) : null}
    </Loading>
  );
}

type ConceptDetailsProps = {
  criteria: ConceptCriteria;
};

function ConceptDetails(props: ConceptDetailsProps) {
  return (
    <>
      {props.criteria.selected.length === 0 ? (
        <Typography variant="body1">None selected</Typography>
      ) : (
        props.criteria.selected.map((row) => (
          <Stack direction="row" alignItems="baseline" key={row.concept_id}>
            <Typography variant="body1">{row.concept_id}</Typography>&nbsp;
            <Typography variant="body2">{row.concept_name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
