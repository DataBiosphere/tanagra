import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { Search } from "components/search";
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
  generateValueDataFilter,
  ValueConfig,
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
import { ClassificationNode, Source } from "data/source";
import { useSource } from "data/sourceContext";
import { DataEntry, DataKey } from "data/types";
import { useIsNewCriteria, useUpdateCriteria } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useEffect, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { CriteriaConfig } from "underlaysSlice";
import { useLocalSearchState } from "util/searchState";
import emptyImage from "../images/empty.svg";

type Selection = {
  key: DataKey;
  name: string;
  classification?: string;

  // Deprecated
  entity?: string;
};

// A custom TreeGridItem allows us to store the ClassificationNode along with
// the rest of the data.
type ClassificationNodeItem = TreeGridItem & {
  node: ClassificationNode;
  classification: string;
};

export interface Config extends CriteriaConfig {
  columns: TreeGridColumn[];
  nameColumnIndex?: number;
  hierarchyColumns?: TreeGridColumn[];
  // TODO(tjennison): Multiple occurrence: Consider converting these to always
  // take an array and updating all of the underlays. For now, firstOf is a nice
  // funnel for tracking the inconsistent handling of multiple occurrences.
  occurrences: string | string[];
  classifications: string[];
  multiSelect?: boolean;
  valueConfigs?: ValueConfig[];
  defaultSort?: SortOrder;
  limit?: number;
}

// Exported for testing purposes.
export interface Data {
  selected: Selection[];
  valueData: ValueData;
}

// "classification" plugins select occurrences based on an occurrence field that
// references another entity, often using hierarchies and/or groupings.
@registerCriteriaPlugin(
  "classification",
  (source: Source, c: CriteriaConfig, dataEntry?: DataEntry) => {
    const config = c as Config;

    const data: Data = {
      selected: [],
      valueData: { ...ANY_VALUE_DATA },
    };

    if (dataEntry) {
      const column = config.columns[config.nameColumnIndex ?? 0];
      data.selected.push({
        key: dataEntry.key,
        name: String(dataEntry[column.key]) ?? "",
        classification:
          String(dataEntry.classification) ?? config.classifications[0],
      });
    }

    return data;
  },
  search
)
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<Data> {
  public data: Data;
  private config: Config;

  constructor(public id: string, config: CriteriaConfig, data: unknown) {
    this.config = config as Config;
    this.data = data as Data;
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
    if (!this.config.valueConfigs) {
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
    if (this.data.selected.length > 0) {
      return {
        title: this.data.selected[0].name,
        standaloneTitle: true,
        additionalText: this.data.selected.slice(1).map((s) => s.name),
      };
    }

    return {
      title: "(any)",
    };
  }

  generateFilter(occurrenceId: string) {
    const filters: Filter[] = [
      {
        type: FilterType.Classification,
        occurrenceId: occurrenceId,
        classificationId:
          this.data.selected[0].classification ??
          this.config.classifications[0],
        keys: this.data.selected.map(({ key }) => key),
      },
    ];

    const valueDataFilter = generateValueDataFilter([this.data.valueData]);
    if (valueDataFilter) {
      filters.push(valueDataFilter);
    }

    return makeArrayFilter({}, filters);
  }

  filterOccurrenceIds() {
    if (typeof this.config.occurrences === "string") {
      return [this.config.occurrences];
    }
    return this.config.occurrences;
  }
}

function keyForNode(node: ClassificationNode): DataKey {
  let key = node.data.key;
  if (node.grouping) {
    key = `${node.grouping}~${key}`;
  }
  return key;
}

type SearchState = {
  // The query entered in the search box.
  query?: string;
  // The classification to show the hierarchy for.
  hierarchyClassification?: string;
  // The ancestor list of the item to view the hierarchy for.
  hierarchy?: DataKey[];
  // The item to highlight and scroll to in the hierarchy.
  highlightId?: DataKey;
};

type ClassificationEditProps = {
  data: Data;
  config: Config;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
};

function ClassificationEdit(props: ClassificationEditProps) {
  const source = useSource();
  const occurrence = source.lookupOccurrence(firstOf(props.config.occurrences));
  const classifications = props.config.classifications.map((c) =>
    source.lookupClassification(firstOf(props.config.occurrences), c)
  );
  const updateCriteria = useUpdateCriteria();
  const isNewCriteria = useIsNewCriteria();

  const [searchState, updateSearchState] = useLocalSearchState<SearchState>();

  useEffect(() => {
    // The extra function works around React defaulting to treating a function
    // as an update function.
    props.setBackAction(() =>
      searchState.hierarchy
        ? () => {
            updateSearchState((data) => {
              data.hierarchy = undefined;
              data.hierarchyClassification = undefined;
              data.highlightId = undefined;
            });
          }
        : undefined
    );
  }, [searchState]);

  const processEntities = useCallback(
    (
      nodes: MergedItem<ClassificationNode>[],
      hierarchy?: DataKey[],
      parent?: DataKey,
      prevData?: TreeGridData
    ) => {
      const data = prevData ?? {};

      const children: DataKey[] = [];
      nodes.forEach(({ source: classification, data: node }) => {
        const rowData: TreeGridRowData = { ...node.data };
        if (node.ancestors) {
          rowData.view_hierarchy = node.ancestors;
        }

        const key = keyForNode(node);
        children.push(key);

        // Copy over existing children in case they're being loaded in
        // parallel.
        let childChildren = data[key]?.children;
        if (!childChildren) {
          if ((!node.grouping && !hierarchy) || node.childCount === 0) {
            childChildren = [];
          }
        }

        const cItem: ClassificationNodeItem = {
          data: rowData,
          children: childChildren,
          node: node,
          classification: classification,
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

  const fetchClassification = useCallback(async () => {
    const searchClassifications = classifications
      .map((c) => c.id)
      .filter(
        (c) =>
          !searchState?.hierarchyClassification ||
          searchState?.hierarchyClassification === c
      );

    let sortOrder = {
      attribute: ROLLUP_COUNT_ATTRIBUTE,
      direction: SortDirection.Desc,
    };

    if (searchState.hierarchyClassification) {
      const classification = source.lookupClassification(
        firstOf(props.config.occurrences),
        searchState.hierarchyClassification
      );
      if (classification.defaultSort) {
        sortOrder = classification.defaultSort;
      }
    } else if (props.config.defaultSort) {
      sortOrder = props.config.defaultSort;
    }

    const raw: [string, ClassificationNode[]][] = await Promise.all(
      searchClassifications.map(async (c) => [
        c,
        (
          await source.searchClassification(attributes, occurrence.id, c, {
            query: !searchState?.hierarchy
              ? searchState?.query ?? ""
              : undefined,
            includeGroupings: !searchState?.hierarchy,
            sortOrder: sortOrder,
            limit: props.config.limit,
          })
        ).nodes,
      ])
    );

    const merged = mergeLists(
      raw,
      props.config.limit ?? 100,
      sortOrder.direction,
      (n) => n.data[sortOrder.attribute]
    );
    return processEntities(merged, searchState?.hierarchy);
  }, [source, attributes, processEntities, searchState]);
  const classificationState = useSWRImmutable(
    {
      component: "Classification",
      occurrenceId: occurrence.id,
      classificationIds: classifications.map((c) => c.id),
      searchState,
      attributes,
    },
    fetchClassification
  );

  // Partially update the state when expanding rows in the hierarchy view.
  const updateData = useCallback(
    (data: TreeGridData) => {
      classificationState.mutate(data, { revalidate: false });
    },
    [classificationState]
  );

  const hierarchyColumns: TreeGridColumn[] = useMemo(
    () => [
      ...(props.config.hierarchyColumns ?? []),
      { key: "t_add_button", width: 60 },
    ],
    [props.config.hierarchyColumns]
  );

  const allColumns: TreeGridColumn[] = useMemo(
    () => [
      ...props.config.columns,
      {
        key: "view_hierarchy",
        width: classifications.reduce((r, c) => r || !!c.hierarchy, false)
          ? 180
          : 60,
      },
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
        <Loading status={classificationState}>
          {!classificationState.data?.root?.children?.length ? (
            <Empty
              minHeight="300px"
              image={emptyImage}
              title="No matches found"
            />
          ) : (
            <TreeGrid
              columns={!!searchState?.hierarchy ? hierarchyColumns : allColumns}
              data={classificationState?.data ?? {}}
              defaultExpanded={searchState?.hierarchy}
              highlightId={searchState?.highlightId}
              rowCustomization={(id: TreeGridId, rowData: TreeGridRowData) => {
                if (!classificationState.data) {
                  return undefined;
                }

                // TODO(tjennison): Make TreeGridData's type generic so we can avoid
                // this type assertion. Also consider passing the TreeGridItem to
                // the callback instead of the TreeGridRowData.
                const item = classificationState.data[
                  id
                ] as ClassificationNodeItem;
                if (!item || item.node.grouping) {
                  return undefined;
                }

                const classification = source.lookupClassification(
                  firstOf(props.config.occurrences),
                  item.classification
                );
                const column = props.config.columns[nameColumnIndex];
                const name = rowData[column.key];
                const newItem = {
                  key: item.node.data.key,
                  name: !!name ? String(name) : "",
                  classification: classification.id,
                };

                const hierarchyButton = (
                  <Button
                    startIcon={<AccountTreeIcon />}
                    onClick={() => {
                      updateSearchState((data: SearchState) => {
                        if (rowData.view_hierarchy) {
                          data.hierarchyClassification = classification?.id;
                          data.hierarchy = rowData.view_hierarchy as DataKey[];
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
                        produce(props.data, (data) => {
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
                          <GridLayout cols colAlign="center">
                            {classification.hierarchy ? hierarchyButton : null}
                            {addButton}
                          </GridLayout>
                        ),
                      },
                    ]
                  : [];

                const hierarchyContent = searchState?.hierarchy
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
                  // TODO(tjennison): Handle duplicate keys across entities.
                  const index = props.data.selected.findIndex(
                    (sel) => item.node.data.key === sel.key
                  );

                  return [
                    {
                      column: nameColumnIndex,
                      prefixElements: (
                        <Checkbox
                          size="small"
                          fontSize="inherit"
                          checked={index > -1}
                          onChange={() => {
                            updateCriteria(
                              produce(props.data, (data) => {
                                if (index > -1) {
                                  data.selected.splice(index, 1);
                                } else {
                                  data.selected.push(newItem);
                                }
                                data.valueData = ANY_VALUE_DATA;
                              })
                            );
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
                const data = classificationState.data;
                if (!data) {
                  return Promise.resolve();
                }

                const item = data[id] as ClassificationNodeItem;
                const key = item?.node ? keyForNode(item.node) : id;
                const classificationId =
                  item?.classification ?? searchState.hierarchyClassification;
                if (!classificationId) {
                  throw new Error("No hierarchy selected.");
                }

                if (item?.node.grouping) {
                  return source
                    .searchGrouping(
                      attributes,
                      occurrence.id,
                      classificationId,
                      item.node,
                      {
                        limit: props.config.limit,
                      }
                    )
                    .then((res) => {
                      updateData(
                        processEntities(
                          res.nodes.map((r) => ({
                            source: classificationId,
                            data: r,
                          })),
                          searchState?.hierarchy,
                          key,
                          data
                        )
                      );
                    });
                } else {
                  return source
                    .searchClassification(
                      attributes,
                      occurrence.id,
                      classificationId,
                      {
                        parent: key,
                        limit: props.config.limit,
                      }
                    )
                    .then((res) => {
                      updateData(
                        processEntities(
                          res.nodes.map((r) => ({
                            source: classificationId,
                            data: r,
                          })),
                          searchState?.hierarchy,
                          key,
                          data
                        )
                      );
                    });
                }
              }}
            />
          )}
        </Loading>
      </GridLayout>
    </GridBox>
  );
}

type ClassificationInlineProps = {
  groupId: string;
  criteriaId: string;
  data: Data;
  config: Config;
};

function ClassificationInline(props: ClassificationInlineProps) {
  const source = useSource();
  const classification = source.lookupClassification(
    firstOf(props.config.occurrences),
    props.data.selected[0].classification ?? props.config.classifications[0]
  );
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  if (!props.config.valueConfigs) {
    return null;
  }

  return (
    <ValueDataEdit
      occurrence={firstOf(props.config.occurrences)}
      entity={props.data.selected[0].entity ?? classification.entity}
      hintKey={props.data.selected[0].key}
      singleValue
      valueConfigs={props.config.valueConfigs}
      valueData={[props.data.valueData]}
      update={(valueData) =>
        updateCriteria(
          produce(props.data, (data) => {
            data.valueData = valueData[0];
          })
        )
      }
    />
  );
}

async function search(
  source: Source,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;
  const results = await Promise.all(
    config.classifications.map((classification) =>
      source
        .searchClassification(
          config.columns.map(({ key }) => key),
          firstOf(config.occurrences),
          classification,
          {
            query,
            sortOrder: config.defaultSort,
          }
        )
        .then((res) =>
          res.nodes.map((node) => ({
            ...node.data,
            classification: classification,
          }))
        )
    )
  );

  return results.flat();
}

function firstOf(value: string | string[]) {
  // TODO(tjennison): Multiple occurrence: This uses the first occurrence
  // instead of somehow taking them all account, assuming that they're
  // interchangeable.
  if (typeof value === "string") {
    return value;
  }
  return value[0];
}
