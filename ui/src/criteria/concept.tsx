import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { EntityInstancesApiContext } from "apiContext";
import { CriteriaConfig, CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridRowData,
} from "components/treegrid";
import { useAsyncWithApi } from "errors";
import { useUnderlay } from "hooks";
import produce from "immer";
import React, { useCallback, useContext, useMemo, useState } from "react";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { isValid } from "util/valid";

type Selection = {
  entity: string;
  id: number;
  name: string;
};

type ListChildrenConfig = {
  entity: string;
  // The path within the filter to set the parent ID for the query (e.g.
  // filter.binaryFilter.attributeValue).
  idPath: string;
  filter: tanagra.Filter;
};

type EntityConfig = {
  name: string;
  selectable?: boolean;
  sourceConcepts?: boolean;
  attributes?: string[];

  // hierarchical indicates whether the entity supports a hierarchical view.
  hierarchical?: boolean;
  // listChildren indicates whether the entity can have children in the list
  // view.
  listChildren?: ListChildrenConfig;
};

interface Config extends CriteriaConfig {
  columns: TreeGridColumn[];
  entities: EntityConfig[];
}

// Exported for testing purposes.
export interface Data extends Config {
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

  renderEdit(dispatchFn: (data: Data) => void) {
    return <ConceptEdit dispatchFn={dispatchFn} data={this.data} />;
  }

  renderDetails() {
    return <ConceptDetails data={this.data} />;
  }

  // Normally, filter generation assumes the concepts are liked to the entityVar
  // via an occurrence table (e.g. person -> condition_occurrence -> condition).
  // fromOccurrence causes entityVar to be treated as the occurrence instead
  // (e.g. condition_occurrence -> condition).
  generateFilter(entityVar: string, fromOccurrence: boolean) {
    if (this.data.selected.length === 0) {
      return null;
    }

    const operands = (e: string) =>
      this.data.selected
        .filter(({ entity }) => entity === e)
        .map(({ id }) => ({
          binaryFilter: {
            attributeVariable: {
              variable: "concept",
              name: "concept_id",
            },
            operator: tanagra.BinaryFilterOperator.DescendantOfInclusive,
            attributeValue: {
              int64Val: id,
            },
          },
        }));

    const occurrenceFilter = (entity: EntityConfig, variable: string) => ({
      relationshipFilter: {
        outerVariable: variable,
        newVariable: "concept",
        newEntity: entity.name,
        filter: {
          arrayFilter: {
            operands: operands(entity.name),
            operator: tanagra.ArrayFilterOperator.Or,
          },
        },
      },
    });

    const ret = {
      arrayFilter: {
        operands: this.data.entities
          .filter(
            (entity) =>
              this.data.selected.findIndex(
                (sel) => sel.entity === entity.name
              ) >= 0
          )
          .map((entity) =>
            fromOccurrence
              ? occurrenceFilter(entity, entityVar)
              : {
                  relationshipFilter: {
                    outerVariable: entityVar,
                    newVariable: "occurrence",
                    newEntity: entity.name + "_occurrence",
                    filter: occurrenceFilter(entity, "occurrence"),
                  },
                }
          ),
        operator: tanagra.ArrayFilterOperator.Or,
      },
    };
    return ret;
  }

  // TODO(tjennison): Split filter generation into separate paths for
  // occurrences and primary entities. This will allow occurrence logic to be
  // centralized and remove the limitation of having a single selectable entity.
  occurrenceEntities() {
    return this.data.entities
      .filter((entity) => entity.selectable)
      .map((entity) => entity.name + "_occurrence");
  }
}

const PATH_ATTRIBUTE = "t_path_concept_id";
const NUM_CHILDREN_ATTRIBUTE = "t_numChildren_concept_id";
const ENTITY_ATTRIBUTE = "t_entity";

type HierarchyState = {
  path: TreeGridId[];
  entity: string;
};

type ConceptEditProps = {
  dispatchFn: (data: Data) => void;
  data: Data;
};

function ConceptEdit(props: ConceptEditProps) {
  const underlay = useUnderlay();

  const [hierarchy, setHierarchy] = useState<HierarchyState | undefined>();
  const [query, setQuery] = useState<string>("");
  const [data, updateData] = useImmer<TreeGridData>({});
  const api = useContext(EntityInstancesApiContext);

  const processEntities = useCallback(
    (
      res: tanagra.SearchEntityInstancesResponse,
      entity: EntityConfig,
      hierarchy?: HierarchyState,
      parentId?: number,
      listChildrenEntity?: EntityConfig
    ) => {
      updateData((data) => {
        const children: TreeGridId[] = [];
        if (res.instances) {
          // TODO(tjennison): Use server side limits.
          res.instances.slice(0, 100).forEach((instance) => {
            const id = instance["concept_id"]?.int64Val || 0;
            if (id === 0) {
              return;
            }

            let path: TreeGridId[] | undefined;
            let hasChildren = false;
            const entityName = listChildrenEntity?.name || entity.name;
            const row: TreeGridRowData = {
              [ENTITY_ATTRIBUTE]: entityName,
            };

            for (const k in instance) {
              const v = instance[k];
              if (k === "standard_concept") {
                row[k] = v ? "Standard" : "Source";
              } else if (!v) {
                row[k] = "";
              } else if (k === PATH_ATTRIBUTE) {
                if (v.stringVal) {
                  path = v.stringVal.split(".").map((id) => +id);
                } else if (v.stringVal === "") {
                  path = [];
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
                <IconButton
                  size="small"
                  onClick={() => {
                    setHierarchy({ path: path || [], entity: entityName });
                  }}
                >
                  <AccountTreeIcon fontSize="inherit" />
                </IconButton>
              );
            }

            children.push(id);

            // Copy over existing children in case they're being loaded in
            // parallel.
            let childChildren = data[id]?.children;
            if (!childChildren) {
              if (!hierarchy) {
                hasChildren = !!entity.listChildren && !listChildrenEntity;
              }
              childChildren = !hasChildren ? [] : undefined;
            }

            data[id] = {
              data: row,
              children: childChildren,
            };
          });
        }

        if (parentId) {
          // Store children even if the data isn't loaded yet.
          data[parentId] = { ...data[parentId], children };
        } else {
          data.root = {
            children: [...(data?.root?.children || []), ...children],
            data: {
              [ENTITY_ATTRIBUTE]: entity.name,
            },
          };
        }
      });
    },
    []
  );

  const fetchEntities = useCallback(() => {
    updateData(() => ({}));
    return Promise.all(
      props.data.entities
        .filter((entity) => !hierarchy || entity.name === hierarchy.entity)
        .map((entity) =>
          api.searchEntityInstances(
            searchRequest(
              props.data.columns,
              entity,
              underlay.name,
              !hierarchy ? query : ""
            )
          )
        )
    ).then((res) => {
      res?.forEach((r, i) =>
        processEntities(r, props.data.entities[i], hierarchy)
      );
    });
  }, [
    api,
    props.data.columns,
    props.data.entities,
    underlay.name,
    processEntities,
    hierarchy,
    query,
  ]);
  const conceptsState = useAsyncWithApi<void>(fetchEntities);

  const hierarchyColumns = [
    {
      key: "concept_name",
      width: "100%",
      title: (
        <Button
          variant="contained"
          onClick={() => {
            setHierarchy(undefined);
          }}
        >
          Return to List
        </Button>
      ),
    },
    { key: "concept_id", width: 120, title: "Concept ID" },
  ];

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...props.data.columns,
      ...(props.data.entities.find((entity) => entity.hierarchical)
        ? [{ key: "view_hierarchy", width: 160, title: "View Hierarchy" }]
        : []),
    ],
    [props.data.columns, props.data.entities]
  );

  return (
    <>
      {!hierarchy && (
        <Search
          placeholder="Search by code or description"
          onSearch={setQuery}
        />
      )}
      <Loading status={conceptsState}>
        <TreeGrid
          columns={hierarchy ? hierarchyColumns : allColumns}
          data={data}
          defaultExpanded={hierarchy?.path}
          prefixElements={(id: TreeGridId, rowData: TreeGridRowData) => {
            const entity = findEntity(props.data.entities, rowData);
            if (!entity.selectable) {
              return null;
            }

            const index = props.data.selected.findIndex(
              (row) => row.entity === entity.name && row.id === id
            );

            return (
              <Checkbox
                size="small"
                fontSize="inherit"
                checked={index > -1}
                onChange={() => {
                  props.dispatchFn(
                    produce(props.data, (data) => {
                      if (index > -1) {
                        data.selected.splice(index, 1);
                      } else {
                        const name = rowData["concept_name"];
                        data.selected.push({
                          entity: entity.name,
                          id: id as number,
                          name: !!name ? String(name) : "",
                        });
                      }
                    })
                  );
                }}
              />
            );
          }}
          loadChildren={(id: TreeGridId) => {
            const entity = findEntity(
              props.data.entities,
              data[id]?.data,
              hierarchy?.entity
            );

            let req: tanagra.SearchEntityInstancesOperationRequest;
            let childEntity: EntityConfig;
            if (entity.listChildren && !hierarchy) {
              childEntity = findEntity(
                props.data.entities,
                undefined,
                entity.listChildren?.entity
              );
              req = listChildrenRequest(
                props.data.columns,
                entity.listChildren,
                childEntity,
                underlay.name,
                id as number
              );
            } else {
              req = searchRequest(
                props.data.columns,
                entity,
                underlay.name,
                "",
                id as number
              );
            }

            return api.searchEntityInstances(req).then((res) => {
              processEntities(
                res,
                entity,
                hierarchy,
                id as number,
                childEntity
              );
            });
          }}
        />
      </Loading>
    </>
  );
}

function findEntity(
  entities: EntityConfig[],
  rowData?: TreeGridRowData,
  defaultEntity?: string
) {
  const entity = entities.find(
    (entity) =>
      (rowData && entity.name === rowData?.[ENTITY_ATTRIBUTE]) ||
      entity.name === defaultEntity
  );
  if (!entity) {
    throw "Unknown entity config: " + rowData;
  }
  return entity;
}

function searchRequest(
  columns: TreeGridColumn[],
  entity: EntityConfig,
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
        operator: entity.sourceConcepts
          ? tanagra.BinaryFilterOperator.Equals
          : tanagra.BinaryFilterOperator.NotEquals,
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
  } else if (entity.hierarchical) {
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

  return {
    entityName: entity.name,
    underlayName: underlay,
    searchEntityInstancesRequest: {
      entityDataset: {
        entityVariable: "c",
        selectedAttributes: attributesForEntity(entity, columns),
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

function listChildrenRequest(
  columns: TreeGridColumn[],
  listChildren: ListChildrenConfig,
  childEntity: EntityConfig,
  underlay: string,
  id: number
) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const filter = produce(listChildren.filter, (draft: any) => {
    const keys = listChildren.idPath.split(".");
    const last = keys.pop();
    if (!last) {
      return;
    }

    keys.forEach((key) => {
      draft = draft[key];
    });
    draft[last].int64Val = id;
  });

  return {
    entityName: listChildren.entity,
    underlayName: underlay,
    searchEntityInstancesRequest: {
      entityDataset: {
        entityVariable: listChildren.entity,
        selectedAttributes: attributesForEntity(childEntity, columns),
        filter,
      },
    },
  };
}

function attributesForEntity(entity: EntityConfig, columns: TreeGridColumn[]) {
  return [
    ...(entity.hierarchical ? [PATH_ATTRIBUTE, NUM_CHILDREN_ATTRIBUTE] : []),
    ...(entity.attributes ? entity.attributes : columns.map(({ key }) => key)),
  ];
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
