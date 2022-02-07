import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { EntityInstancesApiContext } from "apiContext";
import { Cohort, CriteriaPlugin, Group, registerCriteriaPlugin } from "cohort";
import { updateCriteriaData } from "cohortsSlice";
import { useAsyncWithApi } from "errors";
import { useAppDispatch } from "hooks";
import produce from "immer";
import Loading from "loading";
import React, { useCallback, useContext, useState } from "react";
import * as tanagra from "tanagra-api";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridRowData,
} from "treegrid";
import { useImmer } from "use-immer";

type Selection = {
  id: number;
  name: string;
};

type Data = {
  entity: string;
  selected: Selection[];
};

@registerCriteriaPlugin("condition", "Condition", "Contains Conditions Codes")
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data
      ? (data as Data)
      : {
          entity: "condition",
          selected: [],
        };
  }

  renderEdit(cohort: Cohort, group: Group) {
    return (
      <ConceptEdit
        cohort={cohort}
        group={group}
        criteriaId={this.id}
        data={this.data}
      />
    );
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  generateFilter() {
    if (this.data.selected.length === 0) {
      return null;
    }

    const operands = this.data.selected.map(({ id }) => ({
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
}

const fetchedColumns: TreeGridColumn[] = [
  { key: "concept_name", width: "100%", title: "Concept Name" },
  { key: "concept_id", width: 120, title: "Concept ID" },
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
  criteriaId: string;
  data: Data;
};

function ConceptEdit(props: ConceptEditProps) {
  const [hierarchical, setHierarchical] = useState(false);
  const [data, updateData] = useImmer<TreeGridData>({});
  const api = useContext(EntityInstancesApiContext);

  const processEntities = useCallback(
    (res: tanagra.SearchEntityInstancesResponse, id?: number) => {
      updateData((data) => {
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
                <IconButton
                  size="small"
                  onClick={() => {
                    setHierarchical(true);
                  }}
                >
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
            // TODO(tjennison): Add attribute to indicate whether entities
            // have children rather than always loading to check.
            data[id] = { data: row };
          });
        }

        if (id) {
          data[id].children = children;
        } else {
          data.root = { children, data: {} };
        }
      });
    },
    []
  );

  const conceptsState = useAsyncWithApi<void>(
    useCallback(
      () =>
        api
          .searchEntityInstances(
            searchFilter(props.data.entity, props.cohort.underlayName)
          )
          .then((res) => {
            processEntities(res);
          }),
      [api, hierarchical]
    )
  );

  const hierarchyColumns = [
    {
      key: "concept_name",
      width: "100%",
      title: (
        <Button
          variant="contained"
          onClick={() => {
            setHierarchical(false);
          }}
        >
          Return to List
        </Button>
      ),
    },
  ];

  const dispatch = useAppDispatch();

  return (
    <Loading status={conceptsState}>
      <TreeGrid
        columns={hierarchical ? hierarchyColumns : allColumns}
        data={data}
        prefixElements={(id: TreeGridId, rowData: TreeGridRowData) => {
          const index = props.data.selected.findIndex((row) => row.id === id);

          return (
            <Checkbox
              size="small"
              checked={index > -1}
              inputProps={{ "aria-label": "controlled" }}
              onChange={() => {
                dispatch(
                  updateCriteriaData({
                    cohortId: props.cohort.id,
                    groupId: props.group.id,
                    criteriaId: props.criteriaId,
                    data: produce(props.data, (data) => {
                      if (index > -1) {
                        data.selected.splice(index, 1);
                      } else {
                        const name = rowData["concept_name"];
                        data.selected.push({
                          id: id as number,
                          name: !!name ? String(name) : "",
                        });
                      }
                    }),
                  })
                );
              }}
            />
          );
        }}
        loadChildren={
          hierarchical
            ? (id: TreeGridId) => {
                return api
                  .searchEntityInstances(
                    searchFilter(
                      props.data.entity,
                      props.cohort.underlayName,
                      id as number
                    )
                  )
                  .then((res) => {
                    processEntities(res, id as number);
                  });
              }
            : undefined
        }
      />
    </Loading>
  );
}

function isValid<Type>(arg: Type) {
  return arg !== null && typeof arg !== "undefined";
}

function searchFilter(entity: string, underlay: string, id?: number) {
  return {
    entityName: entity,
    underlayName: underlay,
    searchEntityInstancesRequest: {
      entityDataset: {
        entityVariable: "c",
        selectedAttributes: fetchedColumns.map((col) => {
          return col.key;
        }),
        filter: {
          arrayFilter: {
            operands: [
              {
                binaryFilter: {
                  attributeVariable: {
                    name: "standard_concept",
                    variable: "c",
                  },
                  operator: tanagra.BinaryFilterOperator.Equals,
                  attributeValue: {
                    stringVal: "S",
                  },
                },
              },
              ...(id
                ? [
                    {
                      binaryFilter: {
                        attributeVariable: {
                          name: "concept_id",
                          variable: "c",
                        },
                        operator: tanagra.BinaryFilterOperator.ChildOf,
                        attributeValue: {
                          int64Val: id as number,
                        },
                      },
                    },
                  ]
                : []),
            ],
            operator: tanagra.ArrayFilterOperator.And,
          },
        },
      },
    },
  };
}

type ConceptDetailsProps = {
  data: Data;
};

function ConceptDetails(props: ConceptDetailsProps) {
  return (
    <>
      {props.data.selected.length === 0 ? (
        <Typography variant="body1">None selected</Typography>
      ) : (
        props.data.selected.map(({ id, name }) => (
          <Stack direction="row" alignItems="baseline" key={id}>
            <Typography variant="body1">{id}</Typography>&nbsp;
            <Typography variant="body2">{name}</Typography>
          </Stack>
        ))
      )}
    </>
  );
}
