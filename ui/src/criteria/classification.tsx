import * as tanagra from "tanagra-api";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import DeleteIcon from "@mui/icons-material/Delete";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Paper from "@mui/material/Paper";
import Popover from "@mui/material/Popover";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, registerCriteriaPlugin, LookupEntry } from "cohort";
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
} from "components/treeGrid";
import { fromProtoColumns } from "components/treeGridHelpers";
import {
  ANY_VALUE_DATA,
  decodeValueDataOptional,
  encodeValueDataOptional,
  ValueData,
} from "criteria/valueData";
import { ValueDataEdit } from "criteria/valueDataEdit";
import { DEFAULT_SORT_ORDER, fromProtoSortOrder } from "data/configuration";
import { MergedItem, mergeLists } from "data/mergeLists";
import {
  CommonSelectorConfig,
  dataKeyFromProto,
  EntityGroupData,
  EntityNode,
  protoFromDataKey,
  UnderlaySource,
  literalFromDataValue,
  makeBooleanLogicFilter,
} from "data/source";
import {
  convertAttributeStringToDataValue,
  dataKeyToKey,
  keyFromDataKey,
  DataEntry,
  DataKey,
} from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useIsNewCriteria, useUpdateCriteria } from "hooks";
import emptyImage from "images/empty.svg";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/entity_group";
import * as dataProto from "proto/criteriaselector/dataschema/entity_group";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import useSWRImmutable from "swr/immutable";
import { useImmer } from "use-immer";
import { base64ToBytes } from "util/base64";
import { useLocalSearchState } from "util/searchState";
import { isValid } from "util/valid";

type Selection = {
  key: DataKey;
  name: string;
  code: string;
  entityGroup: string;
  valueData?: ValueData;
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
  valueData?: ValueData;
}

// "entityGroup" plugins selects occurrences based on the configuration of one
// or more entity groups and their associated entities.
@registerCriteriaPlugin(
  "entityGroup",
  (
    _underlaySource: UnderlaySource,
    c: CommonSelectorConfig,
    dataEntries?: DataEntry[]
  ) => {
    const config = decodeConfig(c);

    const data: Data = {
      selected: [],
      valueData: { ...ANY_VALUE_DATA },
    };

    if (dataEntries) {
      dataEntries?.forEach((e) => {
        const name = String(e[nameAttribute(config)]);
        const code = String(e.code);
        const entityGroup = String(e.entityGroup);
        if (!name || !entityGroup) {
          throw new Error(
            `Invalid parameters from search [${name}, ${entityGroup}].`
          );
        }

        data.selected.push({
          key: e.key,
          name,
          code,
          entityGroup,
        });
      });
    }

    return encodeData(data);
  },
  search,
  lookup
)
class _ implements CriteriaPlugin<string> {
  public data: string;
  private config: configProto.EntityGroup;

  constructor(
    public id: string,
    selector: CommonSelectorConfig,
    data: string
  ) {
    this.config = decodeConfig(selector);
    this.data = data;
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
        additionalText: decodedData.selected.map((s) => `${s.code} ${s.name}`),
      };
    }

    return {
      title: "(any)",
    };
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

type SearchState = {
  // The query entered in the search box.
  query?: string;
  // The entity group to show the hierarchy for.
  hierarchyEntityGroup?: string;
  // The ancestor list of the item to view the hierarchy for.
  hierarchy?: string[];
  // The item to highlight and scroll to in the hierarchy.
  highlightId?: TreeGridId;
};

type ClassificationEditProps = {
  data: string;
  config: configProto.EntityGroup;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
};

const DEFAULT_LIMIT = 100;

export function ClassificationEdit(props: ClassificationEditProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const isNewCriteria = useIsNewCriteria();

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const [localCriteria, updateLocalCriteria] = useImmer(decodedData);

  // TODO(tjennison): AddByCode creates the possiblity that there can be more
  // than one selected item in single select mode. In order to solve it properly
  // there would need to be an option at the selector level that indicates
  // whether a criteria can accept more than one DataEnrty on creation. For now,
  // the docs recommend not using AddByCode without multi select and this forces
  // multi select behavior in that case.
  const multiSelect =
    props.config.multiSelect || decodedData.selected.length > 1;

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
  }, [updateCriteria, localCriteria, decodedData]);

  const [searchState, updateSearchState] = useLocalSearchState<SearchState>();
  const searchRef = useRef<HTMLDivElement | null>(null);
  const [hierarchySearchAnchor, setHierarchySearchAnchor] =
    useState<HTMLDivElement | null>(null);
  const [hierarchyQuery, setHierarchyQuery] = useState<string | undefined>();

  const [unconfirmedChangesDialog, showUnconfirmedChangesDialog] =
    useSimpleDialog();

  const unconfirmedChangesCallback = useCallback(
    () =>
      showUnconfirmedChangesDialog({
        title: "Unsaved changes",
        text: "Unsaved changes will be lost if you go back without saving.",
        buttons: ["Cancel", "Discard changes", "Save"],
        onButton: (button) => {
          if (button === 1) {
            props.doneAction();
          } else if (button === 2) {
            updateCriteriaFromLocal();
            props.doneAction();
          }
        },
      }),
    [updateCriteriaFromLocal, props, showUnconfirmedChangesDialog]
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

      if (!multiSelect || isDataEqual(decodedData, localCriteria)) {
        return undefined;
      } else {
        return unconfirmedChangesCallback;
      }
    });
  }, [
    searchState,
    localCriteria,
    decodedData,
    multiSelect,
    props,
    unconfirmedChangesCallback,
    updateSearchState,
  ]);

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
      prevData?: TreeGridData<EntityNodeItem>
    ): TreeGridData<EntityNodeItem> => {
      const data = prevData ?? { rows: new Map(), children: [] };

      const children: DataKey[] = [];
      nodes.forEach(({ source: entityGroup, data: node }) => {
        const rowData: TreeGridRowData = { ...node.data };
        if (node.ancestors) {
          rowData.view_hierarchy = node.ancestors.map((a) =>
            dataKeyToKey(a, entityGroup)
          );
        }

        const key = dataKeyToKey(node.data.key, entityGroup);
        children.push(key);

        const group = lookupEntityGroupData(
          groupingEntityGroupData,
          entityGroup
        );

        // Copy over existing children in case they're being loaded in
        // parallel. Grouping children and non-grouping, non-hierarchy nodes
        // always have no children.
        let childChildren = data.rows.get(key)?.children;
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
        data.rows.set(key, cItem);
      });

      if (parent) {
        // Store children even if the data isn't loaded yet.
        data.rows.set(parent, { ...data.rows.get(parent), children });
      } else {
        data.children = [...(data.children ?? []), ...children];
      }

      return data;
    },
    [groupingEntityGroupData]
  );

  const attributes = useMemo(
    () => [
      ...new Set(
        [
          props.config.columns.map(({ key }) => key),
          (props.config.hierarchyColumns ?? []).map(({ key }) => key),
          props.config.nameAttribute,
        ]
          .flat()
          .filter(isValid)
      ),
    ],
    [props.config]
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

  const hierarchyEntityConfig = useMemo(
    () =>
      allEntityGroupConfigs.find(
        (c) => searchState?.hierarchyEntityGroup === c.eg.id
      )?.eg,
    [allEntityGroupConfigs, searchState?.hierarchyEntityGroup]
  );

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
    [
      searchState.hierarchyEntityGroup,
      underlaySource,
      props.config,
      allEntityGroupConfigs,
    ]
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
            fromProtoSortOrder(
              calcSortOrder(c.eg.id, !!searchState?.hierarchy, c.grouping)
            ),
            {
              query: !searchState?.hierarchy
                ? (searchState?.query ?? "")
                : undefined,
              limit: !searchState.hierarchy
                ? (props.config.limit ?? DEFAULT_LIMIT)
                : undefined,
              hierarchy: !!searchState.hierarchyEntityGroup,
            }
          )
        ).nodes,
      ])
    );

    const sortOrder = fromProtoSortOrder(calcSortOrder());
    const classifications = mergeLists(
      raw.filter((_r, i) => !entityGroupConfigs[i].grouping),
      props.config.limit ?? DEFAULT_LIMIT,
      sortOrder.direction,
      (n) => n.data[sortOrder.attribute]
    );
    const groups = raw
      .filter((_r, i) => entityGroupConfigs[i].grouping)
      .map(([eg, nodes]) => nodes.map((n) => ({ source: eg, data: n })));

    const merged = [classifications, ...groups].flat();
    return processEntities(merged, searchState?.hierarchy);
  }, [
    underlaySource,
    attributes,
    processEntities,
    searchState.query,
    searchState.hierarchy,
    searchState.hierarchyEntityGroup,
    allEntityGroupConfigs,
    calcSortOrder,
    props.config,
  ]);
  const instancesState = useSWRImmutable(
    {
      type: "entityGroupInstances",
      entityGroupIds: [
        ...classificationEntityGroupData,
        ...groupingEntityGroupData,
      ].map((eg) => eg.id),
      query: searchState.query,
      hierarchy: searchState.hierarchy,
      hierarchyEntityGroup: searchState.hierarchyEntityGroup,
      attributes,
    },
    fetchInstances
  );

  // Partially update the state when expanding rows in the hierarchy view.
  const updateData = useCallback(
    (data: TreeGridData<EntityNodeItem>) => {
      instancesState.mutate(data, { revalidate: false });
    },
    [instancesState]
  );

  const hierarchyColumns: TreeGridColumn[] = useMemo(
    () => [
      ...(fromProtoColumns(props.config.hierarchyColumns) ?? []),
      ...(!multiSelect ? [{ key: "t_add_button", width: 60 }] : []),
    ],
    [props.config.hierarchyColumns, multiSelect]
  );

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...fromProtoColumns(props.config.columns),
      ...(hasHierarchies || !multiSelect
        ? [
            {
              key: "view_hierarchy",
              width: (hasHierarchies ? 160 : 0) + (!multiSelect ? 40 : 0),
            },
          ]
        : []),
    ],
    [props.config.columns, hasHierarchies, multiSelect]
  );

  return (
    <GridBox
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <GridLayout cols fillCol={0}>
        <GridLayout rows>
          <GridBox
            sx={{
              px: 5,
              py: 3,
              height: "auto",
            }}
          >
            <GridBox
              ref={searchRef}
              sx={{
                height: "auto",
              }}
            >
              <Search
                placeholder="Search by code or description"
                onSearch={(query: string) => {
                  updateSearchState((data: SearchState) => {
                    if (data.hierarchy) {
                      setHierarchyQuery(query);
                      if (query?.length) {
                        setHierarchySearchAnchor(searchRef.current);
                      }
                    } else {
                      data.query = query;
                    }
                  });
                }}
                initialValue={
                  searchState?.hierarchy ? hierarchyQuery : searchState?.query
                }
              />
              <Popover
                open={!!hierarchySearchAnchor}
                anchorEl={hierarchySearchAnchor}
                onClose={() => setHierarchySearchAnchor(null)}
                anchorOrigin={{
                  vertical: "bottom",
                  horizontal: "left",
                }}
                disableAutoFocus
                disableEnforceFocus
              >
                <GridBox
                  sx={{
                    minWidth: "500px",
                    maxWidth: "700px",
                    maxHeight: "300px",
                    overflowY: "auto",
                  }}
                >
                  <HierarchySearchList
                    config={props.config}
                    hierarchyQuery={hierarchyQuery}
                    hierarchyEntityConfig={hierarchyEntityConfig}
                    onClick={(ancestors: string[], highlightId: DataKey) => {
                      setHierarchySearchAnchor(null);
                      updateSearchState((data: SearchState) => {
                        data.hierarchy = ancestors;
                        data.highlightId = highlightId;
                      });
                    }}
                  />
                </GridBox>
              </Popover>
            </GridBox>
          </GridBox>
          <Loading status={instancesState}>
            {!instancesState.data?.children?.length ? (
              <Empty
                minHeight="300px"
                image={emptyImage}
                title="No matches found"
              />
            ) : (
              <TreeGrid
                columns={searchState?.hierarchy ? hierarchyColumns : allColumns}
                data={instancesState?.data ?? {}}
                defaultExpanded={searchState?.hierarchy}
                highlightId={searchState?.highlightId}
                expandable
                reserveExpansionSpacing={!!searchState?.hierarchy}
                rowCustomization={(id, item) => {
                  if (item.groupingNode) {
                    return undefined;
                  }

                  const name = item.data[nameAttribute(props.config)];
                  const code = item.data[codeDisplayAttribute(props.config)];
                  const newItem = {
                    key: item.node.data.key,
                    name: name ? String(name) : "",
                    code: code ? String(code) : "",
                    entityGroup: item.entityGroup,
                  };

                  const entityGroup = underlaySource.lookupEntityGroup(
                    item.entityGroup
                  );

                  const hierarchyButton = (
                    <Button
                      startIcon={<AccountTreeIcon />}
                      onClick={() => {
                        setHierarchyQuery(undefined);
                        updateSearchState((data: SearchState) => {
                          if (item.data.view_hierarchy) {
                            data.hierarchyEntityGroup = item.entityGroup;
                            data.hierarchy = item.data
                              .view_hierarchy as string[];
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
                              {!multiSelect ? addButton : null}
                            </GridLayout>
                          ),
                        },
                      ]
                    : [];

                  const hierarchyContent =
                    searchState?.hierarchy && !multiSelect
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

                  if (multiSelect) {
                    const entityGroupSet = selectedSets.get(item.entityGroup);
                    const found = !!entityGroupSet?.has(item.node.data.key);
                    const foundAncestor = !!item.node.ancestors?.reduce(
                      (acc, cur) => acc || !!entityGroupSet?.has(cur),
                      false
                    );

                    return [
                      {
                        column: props.config.nameColumnIndex ?? 0,
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
                loadChildren={(id) => {
                  const data = instancesState.data;
                  if (!data) {
                    return Promise.resolve();
                  }

                  const findEntityGroupConfig = () => {
                    const item = data.rows.get(id);
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
        {multiSelect ? (
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
                              key={String(s.key)}
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
                              <Typography variant="body2">
                                {s.code} {s.name}
                              </Typography>
                              <IconButton
                                onClick={() =>
                                  updateLocalCriteria((data) => {
                                    data.selected.splice(i, 1);
                                  })
                                }
                                size="small"
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

type HierarchySearchListProps = {
  config: configProto.EntityGroup;
  hierarchyQuery?: string;
  hierarchyEntityConfig?: configProto.EntityGroup_EntityGroupConfig;
  onClick: (ancestors: string[], highlightId: DataKey) => void;
};

export function HierarchySearchList(props: HierarchySearchListProps) {
  const underlaySource = useUnderlaySource();

  const [searchState] = useLocalSearchState<SearchState>();

  const fetchHierarchySearchInstances = useCallback(async () => {
    if (!props.hierarchyEntityConfig || !props.hierarchyQuery) {
      return [];
    }

    return (
      await underlaySource.searchEntityGroup(
        [nameAttribute(props.config)],
        props.hierarchyEntityConfig.id,
        fromProtoSortOrder(props.config.defaultSort ?? DEFAULT_SORT_ORDER),
        {
          query: props.hierarchyQuery,
          limit: DEFAULT_LIMIT,
          hierarchy: true,
        }
      )
    ).nodes;
  }, [
    underlaySource,
    props.hierarchyQuery,
    props.config,
    props.hierarchyEntityConfig,
  ]);
  const hierarchySearchState = useSWRImmutable(
    {
      type: "hierarchySearchInstances",
      query: props.hierarchyQuery,
      hierarchy: searchState?.hierarchy,
      hierarchyEntityGroup: searchState?.hierarchyEntityGroup,
    },
    fetchHierarchySearchInstances
  );

  return (
    <Loading status={hierarchySearchState} immediate>
      {!hierarchySearchState.data?.length ? (
        <Empty title="No matches found" />
      ) : (
        <GridLayout rows height="auto" sx={{ px: 2, py: 1 }}>
          {hierarchySearchState.data?.map((n) => {
            const title = String(n.data?.[nameAttribute(props.config)]);
            return (
              <Link
                key={String(n.data.key)}
                component="button"
                variant="body2"
                color="inherit"
                underline="hover"
                title={title}
                onClick={() => {
                  const entityGroup = props.hierarchyEntityConfig?.id;
                  if (entityGroup) {
                    props.onClick(
                      n.ancestors?.map((a) => dataKeyToKey(a, entityGroup)) ??
                        [],
                      dataKeyToKey(n.data.key, entityGroup)
                    );
                  }
                }}
              >
                {title}
              </Link>
            );
          })}
        </GridLayout>
      )}
    </Loading>
  );
}

type ClassificationInlineProps = {
  groupId: string;
  criteriaId: string;
  data: string;
  config: configProto.EntityGroup;
};

export function ClassificationInline(props: ClassificationInlineProps) {
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
    <GridLayout rows height="auto">
      {decodedData.selected.map((s, i) => (
        <ValueDataEdit
          key={String(s.key)}
          hintEntity={entityGroup.occurrenceEntityIds[0]}
          relatedEntity={entityGroup.selectionEntity.name}
          hintKey={s.key}
          singleValue
          title={s.name}
          valueConfigs={props.config.valueConfigs}
          valueData={s.valueData ? [s.valueData] : undefined}
          update={(valueData) =>
            updateCriteria(
              produce(decodedData, (data) => {
                data.selected[i].valueData = valueData?.[0];
              })
            )
          }
        />
      ))}
    </GridLayout>
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

async function lookup(
  underlaySource: UnderlaySource,
  c: CommonSelectorConfig,
  codes: string[]
): Promise<LookupEntry[]> {
  const config = decodeConfig(c);
  if (!config.codeAttributes?.length) {
    return [];
  }

  const codesSet = new Set(codes);

  const results = await Promise.all(
    (config.classificationEntityGroups ?? []).map((eg) =>
      underlaySource
        .searchEntityGroup(
          config.columns.map(({ key }) => key),
          eg.id,
          fromProtoSortOrder(config.defaultSort ?? DEFAULT_SORT_ORDER),
          {
            filters: generateLookupFilters(underlaySource, config, eg, codes),
            fetchAll: true,
          }
        )
        .then((res) =>
          res.nodes.map((node) => {
            let code: string | undefined;
            for (const a of config.codeAttributes) {
              const val = String(node.data[a]);
              if (codesSet.has(val)) {
                code = val;
              }
            }

            if (!code) {
              throw new Error(
                `Result ${JSON.stringify(
                  node
                )} does not contain a code from ${codes}.`
              );
            }

            return {
              config: c.name,
              code,
              name: String(node.data[nameAttribute(config)]),
              data: {
                ...node.data,
                entityGroup: eg.id,
              },
            };
          })
        )
    )
  );

  return results.flat();
}

function generateLookupFilters(
  underlaySource: UnderlaySource,
  config: configProto.EntityGroup,
  entityGroup: configProto.EntityGroup_EntityGroupConfig,
  codes: string[]
): tanagra.Filter[] | undefined {
  const [entity] = underlaySource.lookupRelatedEntity(entityGroup.id);
  const operands: tanagra.Filter[] = [];

  for (const ca of config.codeAttributes) {
    const attribute = entity.attributes.find((a) => a.name === ca);
    if (!attribute) {
      throw new Error(`Unknown attribute "${ca}" in entity "${entity.name}".`);
    }

    const values: tanagra.Literal[] = [];
    codes.forEach((c) => {
      try {
        values.push(
          literalFromDataValue(convertAttributeStringToDataValue(c, attribute))
        );
      } catch (_e) {
        // Don't attempt to match this attribute if conversion fails.
      }
    });

    operands.push({
      filterType: tanagra.FilterFilterTypeEnum.Attribute,
      filterUnion: {
        attributeFilter: {
          attribute: ca,
          operator: tanagra.AttributeFilterOperatorEnum.In,
          values: values,
        },
      },
    });
  }

  const combined =
    makeBooleanLogicFilter(
      tanagra.BooleanLogicFilterOperatorEnum.Or,
      operands
    ) ?? undefined;
  return combined ? [combined] : undefined;
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
  }, [
    config.classificationEntityGroups,
    config.groupingEntityGroups,
    underlaySource,
  ]);
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
  const message =
    data[0] === "{"
      ? dataProto.EntityGroup.fromJSON(JSON.parse(data))
      : dataProto.EntityGroup.decode(base64ToBytes(data));

  return {
    selected:
      message.selected?.map((s) => ({
        key: dataKeyFromProto(s.key),
        name: s.name,
        code: s.code,
        entityGroup: s.entityGroup,
        valueData:
          decodeValueDataOptional(s.valueData) ??
          decodeValueDataOptional(message.valueData),
      })) ?? [],
  };
}

function encodeData(data: Data): string {
  const message: dataProto.EntityGroup = {
    selected: data.selected.map((s) => ({
      key: protoFromDataKey(s.key),
      name: s.name,
      code: s.code,
      entityGroup: s.entityGroup,
      valueData: encodeValueDataOptional(s.valueData),
    })),
    valueData: undefined,
  };
  return JSON.stringify(dataProto.EntityGroup.toJSON(message));
}

function decodeConfig(selector: CommonSelectorConfig): configProto.EntityGroup {
  return configProto.EntityGroup.fromJSON(JSON.parse(selector.pluginConfig));
}

function nameAttribute(config: configProto.EntityGroup) {
  return (
    config.nameAttribute ?? config.columns[config.nameColumnIndex ?? 0].key
  );
}

function codeDisplayAttribute(config: configProto.EntityGroup) {
  return config.codeDisplayAttribute ?? null;
}
