import DeleteIcon from "@mui/icons-material/Delete";
import { ValueDataEdit } from "criteria/valueDataEdit";
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
import { DEFAULT_SORT_ORDER, fromProtoSortOrder } from "data/configuration";
import {
  CommonSelectorConfig,
  dataKeyFromProto,
  EntityNode,
  protoFromDataKey,
  UnderlaySource,
} from "data/source";
import {
  compareDataValues,
  dataKeyToKey,
  DataEntry,
  DataKey,
} from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUpdateCriteria } from "hooks";
import emptyImage from "images/empty.svg";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/survey";
import * as dataProto from "proto/criteriaselector/dataschema/survey";
import { useCallback, useEffect, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { useImmer } from "use-immer";
import { base64ToBytes } from "util/base64";
import { safeRegExp } from "util/safeRegExp";
import { useLocalSearchState } from "util/searchState";
import { isValid } from "util/valid";

type Selection = {
  key: DataKey;
  name: string;
  entityGroup: string;
  questionKey?: DataKey;
  questionName: string;
  valueData?: ValueData;
};

enum EntityNodeItemType {
  Question = "QUESTION",
  Answer = "ANSWER",
  Topic = "TOPIC",
}

// A custom TreeGridItem allows us to store the EntityNode along with
// the rest of the data.
type EntityNodeItem = TreeGridItem & {
  node: EntityNode;
  entityGroup: string;
  type: EntityNodeItemType;
  parentKey?: DataKey;
};

// Exported for testing purposes.
export interface Data {
  selected: Selection[];
  valueData?: ValueData;
}

// "survey" plugins are designed to handle medium sized (~<100k rows) amount of
// survey data in an optimized fashion.
@registerCriteriaPlugin(
  "survey",
  (
    underlaySource: UnderlaySource,
    c: CommonSelectorConfig,
    dataEntries?: DataEntry[]
  ) => {
    const config = decodeConfig(c);

    const data: Data = {
      selected: [],
      valueData: { ...ANY_VALUE_DATA },
    };

    if (dataEntries?.length) {
      // TODO(tjennison): There's no way to get the question information for
      // answers added via global search. We're currently not showing answers
      // there so this isn't an issue, but if we were, that information would
      // need to be made available at index time.
      data.selected.push(
        ...dataEntries.map((e) => {
          const name = String(e[nameAttribute(config)]);
          const entityGroup = String(e.entityGroup);
          if (!name || !entityGroup) {
            throw new Error(
              `Invalid parameters from search [${name}, ${entityGroup}].`
            );
          }

          return {
            key: e.key,
            name,
            entityGroup,
            questionName: "",
          };
        })
      );
    }

    return encodeData(data);
  },
  search
)
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.Survey;

  constructor(
    public id: string,
    selector: CommonSelectorConfig,
    data: string
  ) {
    this.selector = selector;
    this.config = decodeConfig(selector);
    this.data = data;
  }

  renderEdit(
    doneAction: () => void,
    setBackAction: (action?: () => void) => void
  ) {
    return (
      <SurveyEdit
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
      <SurveyInline
        groupId={groupId}
        criteriaId={this.id}
        data={this.data}
        config={this.config}
      />
    );
  }

  displayDetails() {
    const decodedData = decodeData(this.data);

    const sel = groupSelection(decodedData.selected);
    if (sel.length > 0) {
      return {
        title:
          sel.length > 1
            ? `${sel[0].name} and ${sel.length - 1} more`
            : sel[0].name,
        standaloneTitle: true,
        additionalText: sel.map(
          (s) =>
            s.name +
            (s.children.length
              ? " (" + s.children.map((child) => child.name).join(", ") + ")"
              : "")
        ),
      };
    }

    return {
      title: "(any)",
    };
  }
}

type SearchState = {
  // The query entered in the search box.
  query?: string;
};

type SurveyEditProps = {
  data: string;
  config: configProto.Survey;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
};

export function SurveyEdit(props: SurveyEditProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

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
  }, [updateCriteria, localCriteria, decodedData]);

  const [searchState, updateSearchState] = useLocalSearchState<SearchState>();

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
      if (isDataEqual(decodedData, localCriteria)) {
        return undefined;
      } else {
        return unconfirmedChangesCallback;
      }
    });
  }, [
    searchState,
    localCriteria,
    decodedData,
    props,
    unconfirmedChangesCallback,
  ]);

  const processEntities = useCallback(
    (allEntityGroups: [string, EntityNode[]][]) => {
      const children: TreeGridId[] = [];
      const rows = new Map<TreeGridId, EntityNodeItem>();

      allEntityGroups.forEach(([entityGroup, nodes]) => {
        nodes.forEach((node) => {
          const rowData: TreeGridRowData = { ...node.data };
          const key = dataKeyToKey(node.data.key, entityGroup);

          let parentKey: string | undefined;
          if (node.ancestors?.length) {
            parentKey = dataKeyToKey(node.ancestors[0], entityGroup);
          }

          const cItem: EntityNodeItem = {
            data: rowData,
            children: rows.get(key)?.children ?? [],
            node: node,
            entityGroup,
            type:
              rows.get(key)?.type ??
              (node.childCount === 0
                ? EntityNodeItemType.Answer
                : EntityNodeItemType.Topic),
            parentKey: parentKey,
          };
          rows.set(key, cItem);

          if (parentKey) {
            const parent = rows.get(parentKey);
            if (parent) {
              parent.children?.push(key);
              if (cItem.type === EntityNodeItemType.Answer) {
                parent.type = EntityNodeItemType.Question;
              }
            } else {
              const d = {
                key: parentKey,
              };
              rows.set(parentKey, {
                data: d,
                node: {
                  data: d,
                  entity: "loading",
                },
                entityGroup,
                type:
                  cItem.type === EntityNodeItemType.Answer
                    ? EntityNodeItemType.Question
                    : EntityNodeItemType.Topic,
                children: [key],
              });
            }
          } else {
            children.push(key);
          }
        });
      });

      return {
        rows,
        children,
      };
    },
    []
  );

  const attributes = useMemo(
    () => [
      ...new Set(
        [
          props.config.columns.map(({ key }) => key),
          nameAttribute(props.config),
        ]
          .flat()
          .filter(isValid)
      ),
    ],
    [props.config]
  );

  const calcSortOrder = useCallback(
    (primaryEntityGroupId?: string) => {
      if (primaryEntityGroupId) {
        const egSortOrder = props.config.entityGroups.find(
          (c) => c.id === primaryEntityGroupId
        )?.sortOrder;
        if (egSortOrder) {
          return egSortOrder;
        }
      }

      return props.config.defaultSort ?? DEFAULT_SORT_ORDER;
    },
    [props.config]
  );

  const fetchInstances = useCallback(async () => {
    const raw: [string, EntityNode[]][] = await Promise.all(
      props.config.entityGroups.map(async (c) => [
        c.id,
        (
          await underlaySource.searchEntityGroup(
            attributes,
            c.id,
            fromProtoSortOrder(calcSortOrder(c.id)),
            {
              hierarchy: true,
              fetchAll: true,
            }
          )
        ).nodes,
      ])
    );

    return processEntities(raw);
  }, [
    underlaySource,
    attributes,
    processEntities,
    calcSortOrder,
    props.config,
  ]);

  const instancesState = useSWRImmutable(
    {
      type: "entityGroupInstances",
      entityGroupIds: [...props.config.entityGroups].map((eg) => eg.id),
      attributes,
    },
    fetchInstances
  );

  const [filteredData, defaultExpanded] = useMemo(() => {
    const data = instancesState.data;
    if (!data) {
      return [{ rows: new Map(), children: [] }, []];
    }
    if (!searchState?.query) {
      return [data, data.children];
    }

    // TODO(tjennison): Handle RegExp errors.
    const [re] = safeRegExp(searchState?.query);
    const matched = new Set<TreeGridId>();
    const ancestors = new Set<TreeGridId>();

    const matchNode = (key: TreeGridId) => {
      const node = data.rows.get(key);
      if (!node) {
        throw new Error(`Tree node not found for ${key}.`);
      }

      if (node.type != EntityNodeItemType.Topic) {
        for (const k in node.data) {
          if (re.test(String(node.data[k]))) {
            matched.add(key);
            node.node.ancestors?.forEach((a) =>
              ancestors.add(dataKeyToKey(a, node.entityGroup))
            );
            break;
          }
        }
      }

      node.children?.forEach((c) => matchNode(c));
    };
    matchNode("root");

    const filtered = produce(data, (data) => {
      for (const key in data) {
        const node = data.rows.get(key);
        if (!node) {
          throw new Error(`Tree node not found for ${key}.`);
        }

        node.children =
          node.type === EntityNodeItemType.Question && matched.has(key)
            ? node.children
            : node.children?.filter((c) => ancestors.has(c) || matched.has(c));
      }
    });
    return [filtered, Array.from(ancestors)];
  }, [instancesState.data, searchState?.query]);

  const columns: TreeGridColumn[] = useMemo(
    () => fromProtoColumns(props.config.columns),
    [props.config.columns]
  );

  const groupedSelection = useMemo(
    () => groupSelection(localCriteria.selected ?? []),
    [localCriteria.selected]
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
          <Loading status={instancesState}>
            {!filteredData?.children?.length ? (
              <Empty
                minHeight="300px"
                image={emptyImage}
                title="No matches found"
              />
            ) : (
              <TreeGrid<EntityNodeItem>
                columns={columns}
                data={filteredData}
                expandable
                defaultExpanded={defaultExpanded}
                reserveExpansionSpacing
                rowCustomization={(id: TreeGridId, item) => {
                  const entityGroupSet = selectedSets.get(item.entityGroup);
                  const found = !!entityGroupSet?.has(item.node.data.key);
                  const foundAncestor = !!item.node.ancestors?.reduce(
                    (acc, cur) => acc || !!entityGroupSet?.has(cur),
                    false
                  );

                  return [
                    {
                      column: 0,
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
                                const question =
                                  item.parentKey &&
                                  item.type === EntityNodeItemType.Answer
                                    ? instancesState.data?.rows?.get(
                                        item.parentKey
                                      )
                                    : undefined;
                                const name =
                                  item.data[nameAttribute(props.config)];
                                const questionName =
                                  question?.node?.data?.[
                                    nameAttribute(props.config)
                                  ];
                                data.selected.push({
                                  key: item.node.data.key,
                                  name: name ? String(name) : "",
                                  entityGroup: item.entityGroup,
                                  questionKey: question?.node?.data?.key,
                                  questionName: questionName
                                    ? String(questionName)
                                    : "",
                                });
                              }
                              data.valueData = ANY_VALUE_DATA;
                            });
                          }}
                        />
                      ),
                    },
                  ];
                }}
              />
            )}
          </Loading>
        </GridLayout>
        <GridBox
          sx={{
            p: 1,
            backgroundColor: (theme) => theme.palette.background.default,
          }}
        >
          <GridLayout rows fillRow={0} spacing={1} width="240px">
            <Paper sx={{ p: 1, height: "100%" }}>
              {groupedSelection.length ? (
                <GridLayout rows fillRow={0}>
                  <GridLayout rows>
                    <Typography variant="body1em">Selected items:</Typography>
                    <GridBox
                      sx={{
                        overflowY: "auto",
                      }}
                    >
                      <GridLayout rows sx={{ height: "fit-content" }}>
                        {groupedSelection.map((s, i) => (
                          <GridLayout
                            key={String(s.key)}
                            rows
                            sx={{ height: "fit-content" }}
                          >
                            <GridLayout
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
                              {s.index >= 0 ? (
                                <IconButton
                                  onClick={() =>
                                    updateLocalCriteria((data) => {
                                      data.selected.splice(s.index, 1);
                                    })
                                  }
                                  size="small"
                                >
                                  <DeleteIcon />
                                </IconButton>
                              ) : null}
                            </GridLayout>
                            <GridLayout
                              rows
                              sx={{ pl: 2, height: "fit-content" }}
                            >
                              {s.children.map((child) => (
                                <GridLayout
                                  key={String(child.key)}
                                  cols
                                  fillCol={0}
                                  rowAlign="middle"
                                >
                                  <Typography variant="body2">
                                    {child.name}
                                  </Typography>
                                  {child.index >= 0 ? (
                                    <IconButton
                                      onClick={() =>
                                        updateLocalCriteria((data) => {
                                          data.selected.splice(child.index, 1);
                                        })
                                      }
                                      size="small"
                                    >
                                      <DeleteIcon />
                                    </IconButton>
                                  ) : null}
                                </GridLayout>
                              ))}
                            </GridLayout>
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
      </GridLayout>
      {unconfirmedChangesDialog}
    </GridBox>
  );
}

type SurveyInlineProps = {
  groupId: string;
  criteriaId: string;
  data: string;
  config: configProto.Survey;
};

export function SurveyInline(props: SurveyInlineProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);
  const groupedSelection = useMemo(
    () => groupSelection(decodedData.selected),
    [decodedData]
  );

  if (!props.config.valueConfigs.length || !decodedData.selected.length) {
    return null;
  }

  const entityGroup = underlaySource.lookupEntityGroup(
    decodedData.selected[0].entityGroup
  );

  const contentForItem = (item: GroupedSelectionItem) => {
    if (item.index < 0) {
      return <Typography variant="body2">{item.name}</Typography>;
    }

    const sel = decodedData.selected[item.index];
    return (
      <GridBox sx={{ pb: 1, height: "auto" }}>
        <ValueDataEdit
          key={String(sel.key)}
          hintEntity={entityGroup.occurrenceEntityIds[0]}
          relatedEntity={entityGroup.selectionEntity.name}
          hintKey={sel.key}
          singleValue
          title={sel.name}
          valueConfigs={props.config.valueConfigs}
          valueData={sel.valueData ? [sel.valueData] : undefined}
          update={(valueData) =>
            updateCriteria(
              produce(decodedData, (data) => {
                data.selected[item.index].valueData = valueData?.[0];
              })
            )
          }
        />
      </GridBox>
    );
  };

  return (
    <GridLayout rows height="auto">
      {groupedSelection.map((s, i) => (
        <GridLayout key={String(s.key)} rows height="auto">
          <GridBox
            sx={{
              height: "auto",
              boxShadow:
                i !== 0
                  ? (theme) => `0 -1px 0 ${theme.palette.divider}`
                  : undefined,
            }}
          >
            {contentForItem(s)}
          </GridBox>
          <GridLayout rows height="auto" sx={{ pl: 2 }}>
            {s.children.map((child) => contentForItem(child))}
          </GridLayout>
        </GridLayout>
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
    (config.entityGroups ?? []).map((eg) =>
      underlaySource
        .searchEntityGroup(
          config.columns.map(({ key }) => key),
          eg.id,
          fromProtoSortOrder(config.defaultSort ?? DEFAULT_SORT_ORDER),
          {
            query,
            isLeaf: false,
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
      ? dataProto.Survey.fromJSON(JSON.parse(data))
      : dataProto.Survey.decode(base64ToBytes(data));

  return {
    selected:
      message.selected?.map((s) => ({
        key: dataKeyFromProto(s.key),
        name: s.name,
        entityGroup: s.entityGroup,
        questionKey: s.questionKey
          ? dataKeyFromProto(s.questionKey)
          : undefined,
        questionName: s.questionName,
        valueData:
          decodeValueDataOptional(s.valueData) ??
          decodeValueDataOptional(message.valueData),
      })) ?? [],
  };
}

function encodeData(data: Data): string {
  const message: dataProto.Survey = {
    selected: data.selected.map((s) => ({
      key: protoFromDataKey(s.key),
      name: s.name,
      entityGroup: s.entityGroup,
      questionKey: s.questionKey ? protoFromDataKey(s.questionKey) : undefined,
      questionName: s.questionName,
      valueData: encodeValueDataOptional(s.valueData),
    })),
    valueData: undefined,
  };
  return JSON.stringify(dataProto.Survey.toJSON(message));
}

function decodeConfig(selector: CommonSelectorConfig): configProto.Survey {
  return configProto.Survey.fromJSON(JSON.parse(selector.pluginConfig));
}

function nameAttribute(config: configProto.Survey) {
  return config.nameAttribute ?? config.columns[0].key;
}

type GroupedSelectionItem = {
  index: number;
  key: DataKey;
  name: string;
  children: GroupedSelectionItem[];
};

function groupSelection(selected: Selection[]): GroupedSelectionItem[] {
  const map = new Map<DataKey, GroupedSelectionItem>();

  selected.forEach((s, index) => {
    const item: GroupedSelectionItem = {
      index,
      key: s.key,
      name: s.name,
      children: [],
    };

    if (!s.questionKey) {
      const existing = map.get(s.key);
      if (existing) {
        // An answer and its question are both selected.
        item.children = existing.children;
      }
      map.set(s.key, item);
    } else {
      const question = map.get(s.questionKey);
      if (question) {
        question.children.push(item);
      } else {
        map.set(s.questionKey, {
          index: -1,
          key: s.questionKey,
          name: s.questionName ?? "Unknown",
          children: [item],
        });
      }
    }
  });

  return Array.from(map, ([, item]) => {
    item.children?.sort((a, b) => compareDataValues(a.key, b.key));
    return item;
  }).sort((a, b) => compareDataValues(a.key, b.key));
}
