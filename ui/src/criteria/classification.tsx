import AccountTreeIcon from "@mui/icons-material/AccountTree";
import Button from "@mui/material/Button";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import { CriteriaPlugin, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import { HintDataSelect } from "components/hintDataSelect";
import Loading from "components/loading";
import { DataRange, RangeSlider } from "components/rangeSlider";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
  TreeGridRowData,
} from "components/treegrid";
import { Filter, FilterType, makeArrayFilter } from "data/filter";
import {
  ClassificationNode,
  SearchClassificationResult,
  Source,
} from "data/source";
import { useSource } from "data/sourceContext";
import { DataEntry, DataKey, DataValue } from "data/types";
import { useIsNewCriteria, useUpdateCriteria } from "hooks";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useEffect, useMemo } from "react";
import useSWRImmutable from "swr/immutable";
import { CriteriaConfig } from "underlaysSlice";
import { searchParamsFromData, useSearchData } from "util/searchData";

type Selection = {
  key: DataKey;
  name: string;
};

// A custom TreeGridItem allows us to store the ClassificationNode along with
// the rest of the data.
type ClassificationNodeItem = TreeGridItem & {
  node: ClassificationNode;
};

type ValueConfig = {
  attribute: string;
  title: string;
};

export interface Config extends CriteriaConfig {
  columns: TreeGridColumn[];
  nameColumnIndex?: number;
  hierarchyColumns?: TreeGridColumn[];
  occurrence: string;
  classification: string;
  multiSelect?: boolean;
  valueConfigs?: ValueConfig[];
}

type ValueSelection = {
  value: DataValue;
  name: string;
};

const ANY_VALUE = "t_any";

type ValueData = {
  attribute: string;
  numeric: boolean;
  selected: ValueSelection[];
  range: DataRange;
};

const DEFAULT_VALUE_DATA = {
  attribute: ANY_VALUE,
  numeric: false,
  selected: [],
  range: {
    id: "",
    min: 0,
    max: 0,
  },
};

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
      valueData: { ...DEFAULT_VALUE_DATA },
    };

    if (dataEntry) {
      const column = config.columns[config.nameColumnIndex ?? 0];
      data.selected.push({
        key: dataEntry.key,
        name: String(dataEntry[column.key]) ?? "",
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

  renderEdit(doneAction: () => void, setBackURL: (url?: string) => void) {
    return (
      <ClassificationEdit
        data={this.data}
        config={this.config}
        doneAction={doneAction}
        setBackURL={setBackURL}
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

  generateFilter() {
    const filters: Filter[] = [
      {
        type: FilterType.Classification,
        occurrenceId: this.config.occurrence,
        classificationId: this.config.classification,
        keys: this.data.selected.map(({ key }) => key),
      },
    ];

    if (this.data.valueData.attribute !== ANY_VALUE) {
      const numeric = this.data.valueData.numeric;
      filters.push({
        type: FilterType.Attribute,
        attribute: this.data.valueData.attribute,
        values: !numeric
          ? this.data.valueData.selected.map((s) => s.value)
          : undefined,
        ranges: numeric ? [this.data.valueData.range] : undefined,
      });
    }

    return makeArrayFilter({}, filters);
  }

  filterOccurrenceId() {
    return this.config.occurrence;
  }
}

function keyForNode(node: ClassificationNode): DataKey {
  let key = node.data.key;
  if (node.grouping) {
    key = `${node.grouping}~${key}`;
  }
  return key;
}

type SearchData = {
  // The query entered in the search box.
  query?: string;
  // The ancestor list of the item to view the hierarchy for.
  hierarchy?: DataKey[];
};

type ClassificationEditProps = {
  data: Data;
  config: Config;
  doneAction: () => void;
  setBackURL: (url?: string) => void;
};

function ClassificationEdit(props: ClassificationEditProps) {
  const source = useSource();
  const occurrence = source.lookupOccurrence(props.config.occurrence);
  const classification = source.lookupClassification(
    props.config.occurrence,
    props.config.classification
  );
  const updateCriteria = useUpdateCriteria();
  const isNewCriteria = useIsNewCriteria();

  const [searchData, updateSearchData] = useSearchData<SearchData>();

  useEffect(() => {
    props.setBackURL(
      searchData.hierarchy
        ? `.?${searchParamsFromData({ query: searchData.query })}`
        : undefined
    );
  }, [searchData]);

  const processEntities = useCallback(
    (
      res: SearchClassificationResult,
      hierarchy?: DataKey[],
      parent?: DataKey,
      prevData?: TreeGridData
    ) => {
      const data = prevData ?? {};

      const children: DataKey[] = [];
      res.nodes.forEach((node) => {
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
    [updateSearchData]
  );

  const attributes = useMemo(
    () => props.config.columns.map(({ key }) => key),
    [props.config.columns]
  );

  const fetchClassification = useCallback(() => {
    return source
      .searchClassification(attributes, occurrence.id, classification.id, {
        query: !searchData?.hierarchy ? searchData?.query ?? "" : undefined,
        includeGroupings: !searchData?.hierarchy,
      })
      .then((res) => processEntities(res, searchData?.hierarchy));
  }, [source, attributes, processEntities, searchData]);
  const classificationState = useSWRImmutable(
    {
      component: "Classification",
      occurrenceId: occurrence.id,
      classificationId: classification.id,
      searchData,
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
      { key: "view_hierarchy", width: classification.hierarchy ? 180 : 60 },
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
        {!searchData?.hierarchy && (
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
                updateSearchData((data: SearchData) => {
                  data.query = query;
                });
              }}
              initialValue={searchData?.query}
            />
          </GridBox>
        )}
        <Loading status={classificationState}>
          {!classificationState.data?.root?.children?.length ? (
            <Empty
              minHeight="300px"
              image="/empty.svg"
              title="No matches found"
            />
          ) : (
            <TreeGrid
              columns={!!searchData?.hierarchy ? hierarchyColumns : allColumns}
              data={classificationState?.data ?? {}}
              defaultExpanded={searchData?.hierarchy}
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

                const column = props.config.columns[nameColumnIndex];
                const name = rowData[column.key];
                const newItem = {
                  key: item.node.data.key,
                  name: !!name ? String(name) : "",
                };

                const hierarchyButton = (
                  <Button
                    startIcon={<AccountTreeIcon />}
                    onClick={() => {
                      updateSearchData((data: SearchData) => {
                        if (rowData.view_hierarchy) {
                          data.hierarchy = rowData.view_hierarchy as DataKey[];
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
                          data.valueData = DEFAULT_VALUE_DATA;
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

                const listContent = !searchData?.hierarchy
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

                const hierarchyContent = searchData?.hierarchy
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
                                data.valueData = DEFAULT_VALUE_DATA;
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
                if (item?.node.grouping) {
                  return source
                    .searchGrouping(
                      attributes,
                      occurrence.id,
                      classification.id,
                      item.node
                    )
                    .then((res) => {
                      updateData(
                        processEntities(res, searchData?.hierarchy, key, data)
                      );
                    });
                } else {
                  return source
                    .searchClassification(
                      attributes,
                      occurrence.id,
                      classification.id,
                      {
                        parent: key,
                      }
                    )
                    .then((res) => {
                      updateData(
                        processEntities(res, searchData?.hierarchy, key, data)
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
    props.config.occurrence,
    props.config.classification
  );
  const updateCriteria = useUpdateCriteria(props.groupId, props.criteriaId);

  const hintDataState = useSWRImmutable(
    {
      type: "hintData",
      occurrence: props.config.occurrence,
      entity: classification.entity,
      key: props.data.selected[0].key,
    },
    async (key) => {
      const hintData = props.config.valueConfigs
        ? await source.getAllHintData(key.occurrence, key.entity, key.key)
        : undefined;
      return {
        hintData,
      };
    }
  );

  if (!props.config.valueConfigs) {
    return null;
  }

  const onSelect = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    updateCriteria(
      produce(props.data, (data) => {
        const config = props.config.valueConfigs?.find(
          (c) => c.attribute === sel
        );
        let attribute = ANY_VALUE;
        if (config) {
          attribute = config.attribute;
        }

        if (attribute === data.valueData.attribute) {
          return;
        }

        const newHintData = hintDataState.data?.hintData?.find(
          (hint) => hint.attribute === attribute
        );

        data.valueData = {
          ...data.valueData,
          attribute,
          numeric: !!newHintData?.integerHint,
        };

        if (data.valueData.range.min === 0 && data.valueData.range.max === 0) {
          data.valueData.range = {
            id: "",
            min: newHintData?.integerHint?.min ?? 0,
            max: newHintData?.integerHint?.max ?? 1000,
          };
        }
      })
    );
  };

  const onValueSelect = (sel: ValueSelection[]) => {
    updateCriteria(
      produce(props.data, (data) => {
        data.valueData.selected = sel;
      })
    );
  };

  const onUpdateRange = (
    range: DataRange,
    index: number,
    min: number,
    max: number
  ) => {
    updateCriteria(
      produce(props.data, (data) => {
        data.valueData.range.min = min;
        data.valueData.range.max = max;
      })
    );
  };

  const selectedHintData = hintDataState.data?.hintData?.find(
    (hint) => hint.attribute === props.data.valueData.attribute
  );

  return (
    <Loading status={hintDataState}>
      <GridLayout cols spacing={2} height="auto">
        <FormControl>
          <Select
            value={props.data.valueData.attribute}
            input={<OutlinedInput />}
            disabled={!hintDataState.data?.hintData?.length}
            onChange={onSelect}
          >
            <MenuItem key={ANY_VALUE} value={ANY_VALUE}>
              Any value
            </MenuItem>
            {props.config.valueConfigs?.map((c) =>
              hintDataState.data?.hintData?.find(
                (hint) => hint.attribute === c.attribute
              ) ? (
                <MenuItem key={c.attribute} value={c.attribute}>
                  {c.title}
                </MenuItem>
              ) : null
            )}
          </Select>
        </FormControl>
        {selectedHintData && selectedHintData.enumHintOptions ? (
          <HintDataSelect
            hintData={selectedHintData}
            selected={props.data.valueData.selected}
            onSelect={onValueSelect}
          />
        ) : null}
        {selectedHintData && selectedHintData.integerHint ? (
          <RangeSlider
            index={0}
            minBound={selectedHintData.integerHint.min}
            maxBound={selectedHintData.integerHint.max}
            range={props.data.valueData.range}
            onUpdate={onUpdateRange}
          />
        ) : null}
        <GridBox />
      </GridLayout>
    </Loading>
  );
}

function search(
  source: Source,
  c: CriteriaConfig,
  query: string
): Promise<DataEntry[]> {
  const config = c as Config;
  return source
    .searchClassification(
      config.columns.map(({ key }) => key),
      config.occurrence,
      config.classification,
      {
        query,
      }
    )
    .then((res) => res.nodes.map((node) => node.data));
}
