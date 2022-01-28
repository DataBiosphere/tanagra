import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Checkbox from "@mui/material/Checkbox";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { EntityInstancesApiContext } from "apiContext";
import { Cohort, Criteria, Group } from "cohort";
import { useCohortUpdater } from "cohortUpdaterContext";
import { useAsyncWithApi } from "errors";
import Loading from "loading";
import React, { useCallback, useContext } from "react";
import * as tanagra from "tanagra-api";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridRowData,
} from "treegrid";

type Selection = {
  id: number;
  name: string;
};

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

    const operands = this.selected.map(({ id }) => ({
      binaryFilter: {
        attributeVariable: {
          variable: "co",
          name: "condition_concept_id",
        },
        operator: tanagra.BinaryFilterOperator.DescendantOfInclusive,
        attributeValue: {
          int64Val: id,
        },
      },
    }));

    return {
      arrayFilter: {
        operands,
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };
  }

  selected = new Array<Selection>();
}

const fetchedColumns: TreeGridColumn[] = [
  { key: "concept_name", width: "100%", title: "Concept Name" },
  { key: "concept_id", width: 120, title: "Concept ID" },
  { key: "domain_id", width: 100, title: "Domain" },
  { key: "standard_concept", width: 180, title: "Source/Standard" },
  { key: "vocabulary_id", width: 120, title: "Vocab" },
  { key: "concept_code", width: 120, title: "Code" },
];

const allColumns: TreeGridColumn[] = [
  ...fetchedColumns,
  { key: "view_hierarchy", width: 160, title: "View Hierarchy" },
];

type ConceptEditProps = {
  cohort: Cohort;
  group: Group;
  criteria: ConceptCriteria;
  filter: string;
};

function ConceptEdit(props: ConceptEditProps) {
  const api = useContext(EntityInstancesApiContext);

  const conceptsState = useAsyncWithApi<TreeGridData>(
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
                  return col.key;
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
            const data: TreeGridData = {};
            const children: TreeGridId[] = [];
            if (res.instances) {
              // TODO(tjennison): Use server side limits.
              res.instances.slice(0, 100).forEach((instance) => {
                const id = instance["concept_id"]?.int64Val || 0;
                if (id === 0) {
                  return;
                }

                const row: TreeGridRowData = {
                  view_hierarchy: (
                    <IconButton size="small">
                      <AccountTreeIcon fontSize="inherit" />
                    </IconButton>
                  ),
                };
                for (const k in instance) {
                  const v = instance[k];
                  if (k === "standard_concept") {
                    row[k] = v ? "Standard" : "Source";
                  } else if (!v) {
                    row[k] = "";
                  } else if (isValid(v.int64Val)) {
                    row[k] = v.int64Val;
                  } else if (isValid(v.boolVal)) {
                    row[k] = v.boolVal;
                  } else {
                    row[k] = v.stringVal;
                  }
                }

                children.push(id);
                data[id] = { data: row };
              });
            }
            data.root = { children, data: {} };
            return data;
          }),
      [api]
    )
  );

  const updater = useCohortUpdater();

  return (
    <Loading status={conceptsState}>
      {conceptsState.data ? (
        <TreeGrid
          columns={allColumns}
          data={conceptsState.data}
          prefixElements={(id: TreeGridId, data: TreeGridRowData) => {
            const index = props.criteria.selected.findIndex(
              (row) => row.id === id
            );

            return (
              <Checkbox
                size="small"
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
                        const name = data["concept_name"];
                        criteria.selected.push({
                          id: id as number,
                          name: !!name ? String(name) : "",
                        });
                      }
                    }
                  );
                }}
              />
            );
          }}
        />
      ) : null}
    </Loading>
  );
}

function isValid<Type>(arg: Type) {
  return arg !== null && typeof arg !== "undefined";
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
        props.criteria.selected.map(({ id, name }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">{id}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
