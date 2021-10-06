import Typography from "@mui/material/Typography";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import React, { useContext, useEffect, useMemo, useState } from "react";
import { EntityInstancesApiContext } from "../apiContext";
import { Criteria, DataSet, Group } from "../dataSet";

export class ConceptCriteria extends Criteria {
  constructor(name: string, filter: string) {
    super();
    this.name = name;
    this.filter = filter;
  }

  edit(dataSet: DataSet, group: Group): JSX.Element {
    return (
      <ConceptEdit
        dataSet={dataSet}
        group={group}
        criteria={this}
        filter={this.filter}
      />
    );
  }

  details(): JSX.Element {
    return <Typography variant="body1">Details!</Typography>;
  }

  filter: string;
}

// Row acts as a bridge between the data format returned from the API and the
// format expected by DataGrid.
class Row {
  id = 0;
  [key: string]: string | number | boolean | undefined;
}

type ConceptEditProps = {
  dataSet: DataSet;
  group: Group;
  criteria: ConceptCriteria;
  filter: string;
};

function ConceptEdit(props: ConceptEditProps) {
  const [error, setError] = useState<Error | null>(null);
  const [rows, setRows] = useState<Array<Row> | null>(null);

  const api = useContext(EntityInstancesApiContext);

  const columns: GridColDef[] = useMemo(
    () => [
      { field: "concept_name", headerName: "Concept Name", width: 400 },
      { field: "concept_id", headerName: "Concept ID", width: 100 },
      { field: "domain_id", headerName: "Domain", width: 100 },
    ],
    []
  );

  // TODO(tjennison): Migrate to useAsync.
  useEffect(() => {
    api
      .searchEntityInstances({
        entityName: "concept",
        underlayName: props.dataSet.underlayName,
        searchEntityInstancesRequest: {
          entityDataset: {
            entityVariable: "c",
            selectedAttributes: columns.map((col) => {
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
              res.instances.map((instance, i) => {
                const row: Row = { id: i };
                for (const k in instance) {
                  const v = instance[k];
                  if (!v) {
                    row[k] = "";
                  } else if (!!v.int64Val) {
                    row[k] = v.int64Val;
                  } else if (!!v.boolVal) {
                    row[k] = v.boolVal;
                  } else {
                    row[k] = v.stringVal;
                  }
                }
                return row;
              })
            );
          } else {
            setRows([]);
          }
        },
        (error) => {
          setError(error);
        }
      );
  }, [api, props.filter, props.dataSet.underlayName, columns]);

  if (error) {
    return <div>Error: {error.message}</div>;
  } else if (!rows) {
    return <div>Loading...</div>;
  }

  return <DataGrid autoHeight rows={rows} columns={columns} />;
}
