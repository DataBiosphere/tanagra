import Checkbox from "@mui/material/Checkbox";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import Typography from "@mui/material/Typography";
import {
  DataGrid,
  GridColDef,
  GridRenderCellParams,
  GridRowData,
} from "@mui/x-data-grid";
import { EntityInstancesApiContext } from "apiContext";
import { Criteria, Dataset, Group } from "dataset";
import { useDatasetUpdater } from "datasetUpdaterContext";
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

export class ConceptCriteria extends Criteria {
  constructor(name: string, public filter: string) {
    super(name);
  }

  renderEdit(dataset: Dataset, group: Group): JSX.Element {
    return (
      <ConceptEdit
        dataset={dataset}
        group={group}
        criteria={this}
        filter={this.filter}
      />
    );
  }

  renderDetails(): JSX.Element {
    return <ConceptDetails criteria={this} />;
  }

  selected = new Array<GridRowData>();
}

const fetchedColumns: GridColDef[] = [
  { field: "concept_name", headerName: "Concept Name", width: 400 },
  { field: "concept_id", headerName: "Concept ID", width: 100 },
  { field: "domain_id", headerName: "Domain", width: 100 },
];

type ConceptEditProps = {
  dataset: Dataset;
  group: Group;
  criteria: ConceptCriteria;
  filter: string;
};

function ConceptEdit(props: ConceptEditProps) {
  const [error, setError] = useState<Error | null>(null);
  const [rows, setRows] = useState<Array<GridRowData> | null>(null);

  const api = useContext(EntityInstancesApiContext);

  // TODO(tjennison): Migrate to useAsync.
  useEffect(() => {
    api
      .searchEntityInstances({
        entityName: "concept",
        underlayName: props.dataset.underlayName,
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
      .then(
        (res) => {
          if (res.instances) {
            setRows(
              res.instances
                .map((instance) => {
                  const row: GridRowData = {
                    id: instance["concept_id"]?.int64Val || 0,
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
                .filter((row) => row.id !== 0)
            );
          } else {
            setRows([]);
          }
        },
        (error) => {
          setError(error);
        }
      );
  }, [api, props.filter, props.dataset.underlayName]);

  const updater = useDatasetUpdater();

  const renderSelected = useCallback(
    (params: GridRenderCellParams) => {
      const index = props.criteria.selected.findIndex(
        (row) => row.id === params.row.id
      );
      return (
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
      );
    },
    [updater]
  );

  const columns: GridColDef[] = useMemo(
    () => [
      {
        field: "SELECTED",
        headerName: "",
        width: 50,
        renderCell: renderSelected,
      },
      ...fetchedColumns,
    ],
    [renderSelected]
  );

  if (error) {
    return <div>Error: {error.message}</div>;
  } else if (!rows) {
    return <div>Loading...</div>;
  }

  return <DataGrid autoHeight rows={rows} columns={columns} />;
}

type ConceptDetailsProps = {
  criteria: ConceptCriteria;
};

function ConceptDetails(props: ConceptDetailsProps) {
  return (
    <List dense>
      {props.criteria.selected.map((row) => (
        <ListItem key={row.id}>
          <Typography variant="body1">{row.concept_id}</Typography>&nbsp;
          <Typography variant="body2">{row.concept_name}</Typography>
        </ListItem>
      ))}
    </List>
  );
}
