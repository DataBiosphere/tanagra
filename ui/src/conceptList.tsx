import Typography from "@mui/material/Typography";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import React, { useContext, useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { EntityInstancesApiContext } from "./apiContext";
import { DataSet } from "./dataSet";

// Row acts as a bridge between the data format returned from the API and the
// format expected by DataGrid.
class Row {
  id = 0;
  [key: string]: string | number | boolean | undefined;
}

type ConceptListProps = {
  underlayName: string;
  dataSet: DataSet;
  filter: string;
};

export default function ConceptList(props: ConceptListProps) {
  const [error, setError] = useState<Error | null>(null);
  const [rows, setRows] = useState<Array<Row> | null>(null);

  const params = useParams<{ group: string; criteria: string }>();
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
        underlayName: props.underlayName,
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
  }, [api, props.filter, props.underlayName, columns]);

  if (error) {
    return <div>Error: {error.message}</div>;
  } else if (!rows) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <Typography variant="h4">Concepts</Typography>
      <Typography variant="body1">
        Group {params.group}, criteria {params.criteria}
      </Typography>
      <DataGrid autoHeight rows={rows} columns={columns} />
    </>
  );
}
