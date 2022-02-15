import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { EntityInstancesApiContext } from "apiContext";
import {
  Cohort,
  CriteriaConfig,
  CriteriaPlugin,
  Group,
  registerCriteriaPlugin,
} from "cohort";
import { updateCriteriaData } from "cohortsSlice";
import { useAsyncWithApi } from "errors";
import { useAppDispatch } from "hooks";
import produce from "immer";
import Loading from "loading";
import React, { useCallback, useContext, useMemo, useState } from "react";
import { Search } from "search";
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

interface Config extends CriteriaConfig {
  entity: string;
  hierarchical?: boolean;
}

interface Data extends Config {
  selected: Selection[];
}

@registerCriteriaPlugin("concept", (config: CriteriaConfig) => ({
  ...(config.plugin as Config),
  selected: [],
}))
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;

  constructor(public id: string, data: unknown) {
    this.data = data as Data;
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

const PATH_ATTRIBUTE = "t_path_concept_id";
const NUM_CHILDREN_ATTRIBUTE = "t_numChildren_concept_id";

type ConceptEditProps = {
  cohort: Cohort;
  group: Group;
  criteriaId: string;
  data: Data;
};

function ConceptEdit(props: ConceptEditProps) {
  const [hierarchyPath, setHierarchyPath] = useState<
    TreeGridId[] | undefined
  >();
  const [query, setQuery] = useState<string>("");
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

            let path: number[] | undefined;
            let hasChildren = false;
            const row: TreeGridRowData = {};
            for (const k in instance) {
              const v = instance[k];
              if (k === "standard_concept") {
                row[k] = v ? "Standard" : "Source";
              } else if (!v) {
                row[k] = "";
              } else if (k === PATH_ATTRIBUTE) {
                if (v.stringVal || v.stringVal === "") {
                  path = v.stringVal.split(".").map((id) => +id);
                }
              } else if (k === NUM_CHILDREN_ATTRIBUTE) {
                hasChildren = !!v.int64Val;
              } else if (isValid(v.int64Val)) {
                row[k] = v.int64Val;
              } else if (isValid(v.boolVal)) {
                row[k] = v.boolVal;
              } else {
                row[k] = v.stringVal;
              }
            }

            if (path) {
              row.view_hierarchy = (
                <IconButton size="small" onClick={() => setHierarchyPath(path)}>
                  <AccountTreeIcon fontSize="inherit" />
                </IconButton>
              );
            }

            children.push(id);

            // Copy over existing children in case they're being loaded in
            // parallel.
            data[id] = {
              data: row,
              children: data[id]?.children || (!hasChildren ? [] : undefined),
            };
          });
        }

        if (id) {
          // Store children even if the data isn't loaded yet.
          data[id] = { ...data[id], children };
        } else {
          data.root = { children, data: {} };
        }
      });
    },
    []
  );

  const conceptsState = useAsyncWithApi<void>(
    useCallback(() => {
      updateData(() => ({}));
      return api
        .searchEntityInstances(
          searchFilter(
            props.data,
            props.cohort.underlayName,
            !hierarchyPath ? query : ""
          )
        )
        .then((res) => {
          processEntities(res);
        });
    }, [
      api,
      props.data.entity,
      props.cohort.underlayName,
      processEntities,
      hierarchyPath,
      query,
    ]),
    hierarchyPath
  );

  const hierarchyColumns = [
    {
      key: "concept_name",
      width: "100%",
      title: (
        <Button
          variant="contained"
          onClick={() => {
            setHierarchyPath(undefined);
          }}
        >
          Return to List
        </Button>
      ),
    },
    { key: "concept_id", width: 120, title: "Concept ID" },
  ];

  const dispatch = useAppDispatch();

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...fetchedColumns,
      ...(props.data.hierarchical
        ? [{ key: "view_hierarchy", width: 160, title: "View Hierarchy" }]
        : []),
    ],
    [props.data.hierarchical]
  );

  return (
    <>
      {!hierarchyPath && (
        <Search
          placeholder="Search by code or description"
          onSearch={setQuery}
        />
      )}
      <Loading status={conceptsState}>
        <TreeGrid
          columns={hierarchyPath ? hierarchyColumns : allColumns}
          data={data}
          defaultExpanded={hierarchyPath}
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
            hierarchyPath
              ? (id: TreeGridId) => {
                  return api
                    .searchEntityInstances(
                      searchFilter(
                        props.data,
                        props.cohort.underlayName,
                        "",
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
    </>
  );
}

function isValid<Type>(arg: Type) {
  return arg !== null && typeof arg !== "undefined";
}

function searchFilter(
  data: Data,
  underlay: string,
  query: string,
  id?: number
) {
  const operands: tanagra.Filter[] = [
    {
      binaryFilter: {
        attributeVariable: {
          name: "standard_concept",
          variable: "c",
        },
        operator: tanagra.BinaryFilterOperator.NotEquals,
      },
    },
  ];

  if (id) {
    operands.push({
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
    });
  } else if (query) {
    operands.push({
      textSearchFilter: {
        entityVariable: "c",
        term: query,
      },
    });
  } else if (data.hierarchical) {
    operands.push({
      binaryFilter: {
        attributeVariable: {
          name: "t_path_concept_id",
          variable: "c",
        },
        operator: tanagra.BinaryFilterOperator.Equals,
        attributeValue: {
          stringVal: "",
        },
      },
    });
  }

  const fetchedAttributes = [
    ...(data.hierarchical ? [PATH_ATTRIBUTE, NUM_CHILDREN_ATTRIBUTE] : []),
    ...fetchedColumns.map(({ key }) => key),
  ];

  return {
    entityName: data.entity,
    underlayName: underlay,
    searchEntityInstancesRequest: {
      entityDataset: {
        entityVariable: "c",
        selectedAttributes: fetchedAttributes,
        filter: {
          arrayFilter: {
            operands,
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
