import AccountTreeIcon from "@mui/icons-material/AccountTree";
import DeleteIcon from "@mui/icons-material/Delete";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { Search } from "components/search";
import { useSimpleDialog } from "components/simpleDialog";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
  TreeGridRowData,
} from "components/treegrid";
import {
  ANY_VALUE_DATA,
  decodeValueData,
  encodeValueData,
  generateValueDataFilter,
  ValueData,
  ValueDataEdit,
} from "criteria/valueData";
import {
  ROLLUP_COUNT_ATTRIBUTE,
  SortDirection,
  SortOrder,
} from "data/configuration";
import { Filter, FilterType, makeArrayFilter } from "data/filter";
import { MergedItem, mergeLists } from "data/mergeLists";
import {
  CommonSelectorConfig,
  dataKeyFromProto,
  EntityGroupData,
  EntityNode,
  protoFromDataKey,
  UnderlaySource,
} from "data/source";
import { DataEntry, DataKey } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useIsNewCriteria, useUpdateCriteria } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/entity_group";
import * as dataProto from "proto/criteriaselector/dataschema/entity_group";
import * as sortOrderProto from "proto/criteriaselector/sort_order";
import { useCallback, useEffect, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { useImmer } from "use-immer";
import { base64ToBytes, bytesToBase64 } from "util/base64";
import { useLocalSearchState } from "util/searchState";
import emptyImage from "../images/empty.svg";

type Selection = {
  key: DataKey;
  name: string;
  entityGroup: string;
};

// A custom TreeGridItem allows us to store the EntityNode along with
// the rest of the data.
type EntityNodeItem = TreeGridItem & {
  node: EntityNode;
  entityGroup: string;
  groupingNode: boolean;
};

// Exported for testing purposes.
export interface Data {
  selected: Selection[];
  valueData: ValueData;
}

// "entityGroup" plugins selects occurrences based on the configuration of one
// or more entity groups and their associated entities.
@registerCriteriaPlugin(
  "entityGroup",
  (
    underlaySource: UnderlaySource,
    c: CommonSelectorConfig,
    dataEntry?: DataEntry
  ) => {
    const config = decodeConfig(c);

    const data: Data = {
      selected: [],
      valueData: { ...ANY_VALUE_DATA },
    };

    if (dataEntry) {
      const column = config.columns[config.nameColumnIndex ?? 0];

      const name = String(dataEntry[column.key]);
      const entityGroup = String(dataEntry.entityGroup);
      if (!name || !entityGroup) {
        throw new Error(
          `Invalid parameters from search [${name}, ${entityGroup}].`
        );
      }

      data.selected.push({
        key: dataEntry.key,
        name,
        entityGroup,
      });
    }

    return encodeData(data);
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.EntityGroup;

  constructor(public id: string, selector: CommonSelectorConfig, data: string) {
    this.selector = selector;
    this.config = decodeConfig(selector);
    try {
      this.data = encodeData(JSON.parse(data));
    } catch (e) {
      this.data = data;
    }
  }

  renderEdit(
    doneAction: () => void,
    setBackAction: (action?: () => void) => void
  ) {
    return (
      <ClassificationEdit
        data={this.data}
        config={this.config}
        doneAction={doneAction}
        setBackAction={setBackAction}
      />
    );
  }

  renderInline(groupId: string) {
    if (!this.config.valueConfigs.length) {
      return null;
    }

    return (
      <ClassificationInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    const decodedData = decodeData(this.data);

    const sel = decodedData.selected;
    if (sel.length > 0) {
      return {
        title:
          sel.length > 1
            ? `${sel[0].name} and ${sel.length - 1} more`
            : sel[0].name,
        standaloneTitle: true,
        additionalText: decodedData.selected.slice(1).map((s) => s.name),
      };
    }

    return {
      title: "(any)",
    };
  }

  generateFilter(entityId: string, underlaySource: UnderlaySource) {
    const decodedData = decodeData(this.data);
    const filters: Filter[] = [];

    configEntityGroups(this.config).forEach((c) => {
      const keys = decodedData.selected.filter((s) => s.entityGroup === c.id);
      if (keys.length > 0) {
        const entityGroup = underlaySource.lookupEntityGroup(c.id);
        filters.push({
          type: FilterType.EntityGroup,
          entityGroupId: c.id,
          entityId: entityGroup.relatedEntityId ?? entityGroup.entityId,
          keys: keys.map(({ key }) => key),
        });
      }
    });

    const keyFilter = makeArrayFilter({ min: 1 }, filters);

    const valueDataFilter = generateValueDataFilter([decodedData.valueData]);
    if (valueDataFilter) {
      return makeArrayFilter({}, [keyFilter, valueDataFilter]);
    }

    return keyFilter;
  }

  filterEntityIds(underlaySource: UnderlaySource) {
    return [
      ...new Set(
        configEntityGroups(this.config)
          .map((eg) => {
            return underlaySource.lookupEntityGroup(eg.id).occurrenceEntityIds;
          })
          .flat()
      ),
    ];
  }
}

function dataKey(key: DataKey, entityGroup: string): string {
  return JSON.stringify({
    entityGroup,
    key,
  });
}

function keyFromDataKey(key: string): DataKey {
  const parsed = JSON.parse(key);
  return parsed.key;
}

type SearchState = {
  // The query entered in the search box.
  query?: string;
  // The entity group to show the hierarchy for.
  hierarchyEntityGroup?: string;
  // The ancestor list of the item to view the hierarchy for.
  hierarchy?: string[];
  // The item to highlight and scroll to in the hierarchy.
  highlightId?: DataKey;
};

type ClassificationEditProps = {
  data: string;
  config: configProto.EntityGroup;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
};

const DEFAULT_LIMIT = 100;

function ClassificationEdit(props: ClassificationEditProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const isNewCriteria = useIsNewCriteria();

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const [localCriteria, updateLocalCriteria] = useImmer(decodedData);

  const selectedSets = useMemo(() => {
    const sets = new Map<string, Set<DataKey>>();
    localCriteria.selected.forEach((s) => {
      if (!sets.has(s.entityGroup)) {
        sets.set(s.entityGroup, new Set<DataKey>());
      }
      sets.get(s.entityGroup)?.add(s.key);
    });
    return sets;
  }, [localCriteria]);

  const updateCriteriaFromLocal = useCallback(() => {
    updateCriteria(produce(decodedData, () => localCriteria));
  }, [updateCriteria, localCriteria]);

  const [searchState, updateSearchState] = useLocalSearchState<SearchState>();

  const [unconfirmedChangesDialog, showUnconfirmedChangesDialog] =
    useSimpleDialog();

  const unconfirmedChangesCallback = useCallback(
    () =>
      showUnconfirmedChangesDialog({
        title: "Unsaved changes",
        text: "Unsaved changes will be lost if you go back without saving.",
        buttons: ["Cancel", "Go back", "Save"],
        onButton: (button) => {
          if (button === 1) {
            props.doneAction();
          } else if (button === 2) {
            updateCriteriaFromLocal();
            props.doneAction();
          }
        },
      }),
    [updateCriteriaFromLocal]
  );

  useEffect(() => {
    // The extra function works around React defaulting to treating a function
    // as an update function.
    props.setBackAction(() => {
      if (searchState.hierarchy) {
        return () => {
          updateSearchState((data) => {
            data.hierarchy = undefined;
            data.hierarchyEntityGroup = undefined;
            data.highlightId = undefined;
          });
        };
      }

      if (
        !props.config.multiSelect ||
        isDataEqual(decodedData, localCriteria)
      ) {
        return undefined;
      } else {
        return unconfirmedChangesCallback;
      }
    });
  }, [searchState, localCriteria]);

  const [
    hasHierarchies,
    classificationEntityGroupData,
    groupingEntityGroupData,
  ] = useEntityData(props.config);

  const processEntities = useCallback(
    (
      nodes: MergedItem<EntityNode>[],
      hierarchy?: DataKey[],
      parent?: DataKey,
      prevData?: TreeGridData
    ) => {
      const data = prevData ?? {};

      const children: DataKey[] = [];
      nodes.forEach(({ source: entityGroup, data: node }) => {
        const rowData: TreeGridRowData = { ...node.data };
        if (node.ancestors) {
          rowData.view_hierarchy = node.ancestors.map((a) =>
            dataKey(a, entityGroup)
          );
        }

        const key = dataKey(node.data.key, entityGroup);
        children.push(key);

        const group = lookupEntityGroupData(
          groupingEntityGroupData,
          entityGroup
        );

        // Copy over existing children in case they're being loaded in
        // parallel. Grouping children and non-grouping, non-hierarchy nodes
        // always have no children.
        let childChildren = data[key]?.children;
        if (!childChildren) {
          if (
            (!hierarchy && group && parent) ||
            (!hierarchy && !group) ||
            node.childCount === 0
          ) {
            childChildren = [];
          }
        }

        const cItem: EntityNodeItem = {
          data: rowData,
          children: childChildren,
          node: node,
          entityGroup,
          groupingNode: !!group && !parent,
        };
        data[key] = cItem;
      });

      if (parent) {
        // Store children even if the data isn't loaded yet.
        data[parent] = { ...data[parent], children };
      } else {
        data.root = {
          children: [...(data?.root?.children || []), ...children],
          data: {},
        };
      }

      return data;
    },
    [updateSearchState]
  );

  const attributes = useMemo(
    () => [
      ...new Set(
        [
          props.config.columns.map(({ key }) => key),
          (props.config.hierarchyColumns ?? []).map(({ key }) => key),
        ].flat()
      ),
    ],
    [props.config.columns]
  );

  const allEntityGroupConfigs = useMemo(() => {
    const configs: {
      eg: configProto.EntityGroup_EntityGroupConfig;
      grouping: boolean;
    }[] = [];
    (props.config.classificationEntityGroups ?? []).forEach((eg) =>
      configs.push({
        eg,
        grouping: false,
      })
    );
    (props.config.groupingEntityGroups ?? []).forEach((eg) =>
      configs.push({
        eg,
        grouping: true,
      })
    );
    return configs;
  }, [
    props.config.classificationEntityGroups,
    props.config.groupingEntityGroups,
  ]);

  const calcSortOrder = useCallback(
    (
      primaryEntityGroupId?: string,
      loadChildren?: boolean,
      grouping?: boolean
    ) => {
      let entityGroupId = primaryEntityGroupId;

      if (searchState.hierarchyEntityGroup || loadChildren) {
        const entityGroup = underlaySource.lookupEntityGroup(
          searchState.hierarchyEntityGroup ?? entityGroupId ?? ""
        );
        if (entityGroup.relatedEntityGroupId) {
          entityGroupId = entityGroup.relatedEntityGroupId;
        }
      }

      if (grouping || loadChildren) {
        const egSortOrder = allEntityGroupConfigs.find(
          (c) => c.eg.id === entityGroupId
        )?.eg?.sortOrder;
        if (egSortOrder) {
          return egSortOrder;
        }
      }

      return props.config.defaultSort ?? DEFAULT_SORT_ORDER;
    },
    [searchState.hierarchyEntityGroup, underlaySource]
  );

  const fetchInstances = useCallback(async () => {
    const entityGroupConfigs = allEntityGroupConfigs.filter(
      (c) =>
        !searchState?.hierarchyEntityGroup ||
        searchState?.hierarchyEntityGroup === c.eg.id
    );

    const raw: [string, EntityNode[]][] = await Promise.all(
      entityGroupConfigs.map(async (c) => [
        c.eg.id,
        (
          await underlaySource.searchEntityGroup(
            attributes,
            c.eg.id,
            fromProtoSortOrder(calcSortOrder(c.eg.id, false, c.grouping)),
            {
              query: !searchState?.hierarchy
                ? searchState?.query ?? ""
                : undefined,
              limit: !searchState.hierarchy
                ? props.config.limit ?? DEFAULT_LIMIT
                : undefined,
              hierarchy: !!searchState.hierarchyEntityGroup,
            }
          )
        ).nodes,
      ])
    );

    const sortOrder = fromProtoSortOrder(calcSortOrder());
    const classifications = mergeLists(
      raw.filter((r, i) => !entityGroupConfigs[i].grouping),
      props.config.limit ?? DEFAULT_LIMIT,
      sortOrder.direction,
      (n) => n.data[sortOrder.attribute]
    );
    const groups = raw
      .filter((r, i) => entityGroupConfigs[i].grouping)
      .map(([eg, nodes]) => nodes.map((n) => ({ source: eg, data: n })));

    const merged = [classifications, ...groups].flat();
    return processEntities(merged, searchState?.hierarchy);
  }, [underlaySource, attributes, processEntities, searchState]);
  const instancesState = useSWRImmutable(
    {
      type: "entityGroupInstances",
      entityGroupIds: [
        ...classificationEntityGroupData,
        ...groupingEntityGroupData,
      ].map((eg) => eg.id),
      searchState,
      attributes,
    },
    fetchInstances
  );

  // Partially update the state when expanding rows in the hierarchy view.
  const updateData = useCallback(
    (data: TreeGridData) => {
      instancesState.mutate(data, { revalidate: false });
    },
    [instancesState]
  );

  const hierarchyColumns: TreeGridColumn[] = useMemo(
    () => [
      ...(fromProtoColumns(props.config.hierarchyColumns) ?? []),
      ...(!props.config.multiSelect
        ? [{ key: "t_add_button", width: 60 }]
        : []),
    ],
    [props.config.hierarchyColumns]
  );

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...fromProtoColumns(props.config.columns),
      ...(hasHierarchies || !props.config.multiSelect
        ? [
            {
              key: "view_hierarchy",
              width:
                (hasHierarchies ? 160 : 0) +
                (!props.config.multiSelect ? 40 : 0),
            },
          ]
        : []),
    ],
    [props.config.columns]
  );

  const nameColumnIndex = props.config.nameColumnIndex ?? 0;

  return (
    <GridBox
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <GridLayout cols fillCol={0}>
        <GridLayout rows>
          {!searchState?.hierarchy && (
            <GridBox
              sx={{
                px: 5,
                py: 3,
                height: "auto",
              }}
            >
              <Search
                placeholder="Search by code or description"
                onSearch={(query: string) => {
                  updateSearchState((data: SearchState) => {
                    data.query = query;
                  });
                }}
                initialValue={searchState?.query}
              />
            </GridBox>
          )}
          <Loading status={instancesState}>
            {!instancesState.data?.root?.children?.length ? (
              <Empty
                minHeight="300px"
                image={emptyImage}
                title="No matches found"
              />
            ) : (
              <TreeGrid
                columns={
                  !!searchState?.hierarchy ? hierarchyColumns : allColumns
                }
                data={instancesState?.data ?? {}}
                defaultExpanded={searchState?.hierarchy}
                highlightId={searchState?.highlightId}
                expandable
                reserveExpansionSpacing={!!searchState?.hierarchy}
                rowCustomization={(
                  id: TreeGridId,
                  rowData: TreeGridRowData
                ) => {
                  if (!instancesState.data) {
                    return undefined;
                  }

                  // TODO(tjennison): Make TreeGridData's type generic so we can
                  // avoid this type assertion. Also consider passing the
                  // TreeGridItem to the callback instead of the TreeGridRowData.
                  const item = instancesState.data[id] as EntityNodeItem;
                  if (!item || item.groupingNode) {
                    return undefined;
                  }

                  const column = props.config.columns[nameColumnIndex];
                  const name = rowData[column.key];
                  const newItem = {
                    key: item.node.data.key,
                    name: !!name ? String(name) : "",
                    entityGroup: item.entityGroup,
                  };

                  const entityGroup = underlaySource.lookupEntityGroup(
                    item.entityGroup
                  );

                  const hierarchyButton = (
                    <Button
                      startIcon={<AccountTreeIcon />}
                      onClick={() => {
                        updateSearchState((data: SearchState) => {
                          if (rowData.view_hierarchy) {
                            data.hierarchyEntityGroup = item.entityGroup;
                            data.hierarchy = rowData.view_hierarchy as string[];
                            data.highlightId = id;
                          }
                        });
                      }}
                    >
                      View in hierarchy
                    </Button>
                  );

                  const addButton = (
                    <Button
                      data-testid={name}
                      onClick={() => {
                        updateCriteria(
                          produce(decodedData, (data) => {
                            data.selected = [newItem];
                            data.valueData = ANY_VALUE_DATA;
                          })
                        );
                        props.doneAction();
                      }}
                      variant="outlined"
                      sx={{ minWidth: "auto" }}
                    >
                      {isNewCriteria ? "Add" : "Update"}
                    </Button>
                  );

                  const listContent = !searchState?.hierarchy
                    ? [
                        {
                          column: props.config.columns.length,
                          content: (
                            <GridLayout cols spacing={1} colAlign="center">
                              {entityGroup.selectionEntity.hierarchies?.length
                                ? hierarchyButton
                                : null}
                              {!props.config.multiSelect ? addButton : null}
                            </GridLayout>
                          ),
                        },
                      ]
                    : [];

                  const hierarchyContent =
                    searchState?.hierarchy && !props.config.multiSelect
                      ? [
                          {
                            column: hierarchyColumns.length - 1,
                            content: (
                              <GridLayout cols colAlign="center">
                                {addButton}
                              </GridLayout>
                            ),
                          },
                        ]
                      : [];

                  if (props.config.multiSelect) {
                    const entityGroupSet = selectedSets.get(item.entityGroup);
                    const found = !!entityGroupSet?.has(item.node.data.key);
                    const foundAncestor = !!item.node.ancestors?.reduce(
                      (acc, cur) => acc || !!entityGroupSet?.has(cur),
                      false
                    );

                    return [
                      {
                        column: nameColumnIndex,
                        prefixElements: (
                          <Checkbox
                            size="small"
                            fontSize="inherit"
                            checked={found || foundAncestor}
                            faded={!found && foundAncestor}
                            onChange={() => {
                              updateLocalCriteria((data) => {
                                if (found) {
                                  data.selected = data.selected.filter(
                                    (s) =>
                                      item.node.data.key !== s.key ||
                                      item.entityGroup !== s.entityGroup
                                  );
                                } else {
                                  data.selected.push(newItem);
                                }
                                data.valueData = ANY_VALUE_DATA;
                              });
                            }}
                          />
                        ),
                      },
                      ...listContent,
                      ...hierarchyContent,
                    ];
                  }

                  return [...listContent, ...hierarchyContent];
                }}
                loadChildren={(id: TreeGridId) => {
                  const data = instancesState.data;
                  if (!data) {
                    return Promise.resolve();
                  }

                  const findEntityGroupConfig = () => {
                    const item = data[id] as EntityNodeItem;
                    if (item) {
                      const config = configEntityGroups(props.config).find(
                        (eg) => eg.id === item.entityGroup
                      );
                      if (!config) {
                        throw new Error(
                          `Unexpected entity group ${item.entityGroup}.`
                        );
                      }
                      return config;
                    }

                    const config = configEntityGroups(props.config).find(
                      (eg) => eg.id === searchState?.hierarchyEntityGroup
                    );
                    if (!config) {
                      throw new Error(
                        `Unexpected hierarchy entity group ${searchState?.hierarchyEntityGroup}.`
                      );
                    }
                    return config;
                  };

                  const entityGroupConfig = findEntityGroupConfig();

                  return underlaySource
                    .searchEntityGroup(
                      attributes,
                      entityGroupConfig.id,
                      fromProtoSortOrder(
                        calcSortOrder(entityGroupConfig.id, true)
                      ),
                      {
                        parent: keyFromDataKey(id as string),
                        limit: props.config.limit,
                        hierarchy: !!searchState.hierarchyEntityGroup,
                      }
                    )
                    .then((res) => {
                      updateData(
                        processEntities(
                          res.nodes.map((r) => ({
                            source: entityGroupConfig.id,
                            data: r,
                          })),
                          searchState?.hierarchy,
                          id,
                          data
                        )
                      );
                    });
                }}
              />
            )}
          </Loading>
        </GridLayout>
        {props.config.multiSelect ? (
          <GridBox
            sx={{
              p: 1,
              backgroundColor: (theme) => theme.palette.background.default,
            }}
          >
            <GridLayout rows fillRow={0} spacing={1} width="240px">
              <Paper sx={{ p: 1, height: "100%" }}>
                {localCriteria.selected?.length ? (
                  <GridLayout rows fillRow={0}>
                    <GridLayout rows>
                      <Typography variant="body1em">Selected items:</Typography>
                      <GridBox
                        sx={{
                          overflowY: "auto",
                        }}
                      >
                        <GridLayout rows sx={{ height: "fit-content" }}>
                          {localCriteria.selected.map((s, i) => (
                            <GridLayout
                              key={s.key}
                              cols
                              fillCol={0}
                              rowAlign="middle"
                              sx={{
                                boxShadow:
                                  i !== 0
                                    ? (theme) =>
                                        `0 -1px 0 ${theme.palette.divider}`
                                    : undefined,
                              }}
                            >
                              <Typography variant="body2">{s.name}</Typography>
                              <IconButton
                                onClick={() =>
                                  updateLocalCriteria((data) => {
                                    data.selected.splice(i, 1);
                                  })
                                }
                              >
                                <DeleteIcon />
                              </IconButton>
                            </GridLayout>
                          ))}
                        </GridLayout>
                      </GridBox>
                    </GridLayout>
                  </GridLayout>
                ) : (
                  <Empty minHeight="300px" title="No items selected" />
                )}
              </Paper>
              <GridLayout colAlign="right">
                <Button
                  variant="contained"
                  size="large"
                  onClick={() => {
                    updateCriteriaFromLocal();
                    props.doneAction();
                  }}
                >
                  Save criteria
                </Button>
              </GridLayout>
            </GridLayout>
          </GridBox>
        ) : null}
      </GridLayout>
      {unconfirmedChangesDialog}
    </GridBox>
  );
}

type ClassificationInlineProps = {
  groupId: string;
  criteriaId: string;
  data: string;
  config: configProto.EntityGroup;
};

function ClassificationInline(props: ClassificationInlineProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  if (!props.config.valueConfigs.length) {
    return null;
  }

  const entityGroup = underlaySource.lookupEntityGroup(
    decodedData.selected[0].entityGroup
  );

  return (
    <ValueDataEdit
      hintEntity={entityGroup.occurrenceEntityIds[0]}
      relatedEntity={entityGroup.selectionEntity.name}
      hintKey={decodedData.selected[0].key}
      singleValue
      valueConfigs={props.config.valueConfigs}
      valueData={[decodedData.valueData]}
      update={(valueData) =>
        updateCriteria(
          produce(decodedData, (data) => {
            data.valueData = valueData[0];
          })
        )
      }
    />
  );
}

async function search(
  underlaySource: UnderlaySource,
  c: CommonSelectorConfig,
  query: string
): Promise<DataEntry[]> {
  const config = decodeConfig(c);
  const results = await Promise.all(
    (config.classificationEntityGroups ?? []).map((eg) =>
      underlaySource
        .searchEntityGroup(
          config.columns.map(({ key }) => key),
          eg.id,
          fromProtoSortOrder(config.defaultSort ?? DEFAULT_SORT_ORDER),
          {
            query,
          }
        )
        .then((res) =>
          res.nodes.map((node) => ({
            ...node.data,
            entityGroup: eg.id,
          }))
        )
    )
  );

  return results.flat();
}

function useEntityData(
  config: configProto.EntityGroup
): [boolean, EntityGroupData[], EntityGroupData[]] {
  const underlaySource = useUnderlaySource();

  return useMemo(() => {
    const classificationEntityGroupData = (
      config.classificationEntityGroups ?? []
    ).map((eg) => underlaySource.lookupEntityGroup(eg.id));

    const groupingEntityGroupData = (config.groupingEntityGroups ?? []).map(
      (eg) => underlaySource.lookupEntityGroup(eg.id)
    );

    const hasHierarchies = [
      ...classificationEntityGroupData,
      ...groupingEntityGroupData,
    ].reduce((h, eg) => h || !!eg.selectionEntity.hierarchies?.length, false);

    return [
      hasHierarchies,
      classificationEntityGroupData,
      groupingEntityGroupData,
    ];
  }, [config.classificationEntityGroups, config.groupingEntityGroups]);
}

function lookupEntityGroupData(list: EntityGroupData[], id: string) {
  return list.find((eg) => eg.id === id);
}

function configEntityGroups(config: configProto.EntityGroup) {
  const entityGroups = [
    ...(config.classificationEntityGroups ?? []),
    ...(config.groupingEntityGroups ?? []),
  ];
  if (!entityGroups) {
    throw new Error("At least one entity group must be configured.");
  }
  return entityGroups;
}

function isDataEqual(data1: Data, data2: Data) {
  // TODO(tjennison): In future the ValueData may need to be compared as well.
  if (data1.selected.length != data2.selected.length) {
    return false;
  }
  return data1.selected.reduce(
    (acc, cur, i) =>
      acc &&
      cur.key === data2.selected[i].key &&
      cur.entityGroup === data2.selected[i].entityGroup,
    true
  );
}

function decodeData(data: string): Data {
  const message = dataProto.EntityGroup.decode(base64ToBytes(data));
  return {
    selected:
      message.selected?.map((s) => ({
        key: dataKeyFromProto(s.key),
        name: s.name,
        entityGroup: s.entityGroup,
      })) ?? [],
    valueData: decodeValueData(message.valueData),
  };
}

function encodeData(data: Data): string {
  const message: dataProto.EntityGroup = {
    selected: data.selected.map((s) => ({
      key: protoFromDataKey(s.key),
      name: s.name,
      entityGroup: s.entityGroup,
    })),
    valueData: encodeValueData(data.valueData),
  };
  return bytesToBase64(dataProto.EntityGroup.encode(message).finish());
}

const DEFAULT_SORT_ORDER = {
  attribute: ROLLUP_COUNT_ATTRIBUTE,
  direction: sortOrderProto.SortOrder_Direction.SORT_ORDER_DIRECTION_DESCENDING,
};

function decodeConfig(selector: CommonSelectorConfig): configProto.EntityGroup {
  return configProto.EntityGroup.fromJSON(JSON.parse(selector.pluginConfig));
}

function fromProtoSortOrder(sortOrder: sortOrderProto.SortOrder): SortOrder {
  return {
    attribute: sortOrder.attribute,
    direction:
      sortOrder.direction ===
      sortOrderProto.SortOrder_Direction.SORT_ORDER_DIRECTION_DESCENDING
        ? SortDirection.Desc
        : SortDirection.Asc,
  };
}

function fromProtoColumns(
  columns: configProto.EntityGroup_Column[]
): TreeGridColumn[] {
  return columns.map((c) => ({
    key: c.key,
    width: c.widthString ?? c.widthDouble ?? 100,
    title: c.title,
    sortable: c.sortable,
    filterable: c.filterable,
  }));
}
