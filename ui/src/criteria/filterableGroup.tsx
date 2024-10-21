import AddIcon from "@mui/icons-material/Add";
import ClearIcon from "@mui/icons-material/Clear";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import FilterListIcon from "@mui/icons-material/FilterList";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Popover from "@mui/material/Popover";
import TablePagination from "@mui/material/TablePagination";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { CriteriaPlugin, generateId, registerCriteriaPlugin } from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import { containedIconButtonSx } from "components/iconButton";
import Loading from "components/loading";
import { Search } from "components/search";
import { useSimpleDialog } from "components/simpleDialog";
import {
  fromProtoColumns,
  TreeGrid,
  TreeGridColumn,
  TreeGridId,
  TreeGridRowData,
  useArrayAsTreeGridData,
} from "components/treegrid";
import {
  decodeValueData,
  encodeValueData,
  ValueData,
  ValueDataEdit,
} from "criteria/valueData";
import { DEFAULT_SORT_ORDER, fromProtoSortOrder } from "data/configuration";
import {
  CommonSelectorConfig,
  dataKeyFromProto,
  EntityNode,
  HintData,
  literalFromDataValue,
  makeBooleanLogicFilter,
  protoFromDataKey,
} from "data/source";
import { DataKey } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUpdateCriteria } from "hooks";
import emptyImage from "images/empty.svg";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/criteriaselector/configschema/filterable_group";
import * as dataProto from "proto/criteriaselector/dataschema/filterable_group";
import {
  ChangeEvent,
  MouseEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import useSWRInfinite from "swr/infinite";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { base64ToBytes } from "util/base64";
import { useLocalSearchState } from "util/searchState";
import { isValid } from "util/valid";

type SingleSelect = {
  key: DataKey;
  name: string;
};

type SelectAll = {
  query: string;
  values: ValueData[];
  exclusions: SingleSelect[];
};

type Selection = {
  id: string;
  single?: SingleSelect;
  all?: SelectAll;
};

function selectionTitle(selection: Selection): string {
  if (selection.all?.query) {
    return "Select all group: " + selection.all?.query;
  } else if (selection.single?.name) {
    return "Variant: " + selection.single?.name;
  }
  return "Unknown";
}

interface Data {
  selected: Selection[];
}

// "filterableGroup" plugins allow a GroupItems entity group to be filtered by
// multiple attributes.
@registerCriteriaPlugin("filterableGroup", () => {
  return encodeData({
    selected: [],
  });
})
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.FilterableGroup;

  constructor(public id: string, selector: CommonSelectorConfig, data: string) {
    this.selector = selector;
    this.config = decodeConfig(selector);
    this.data = data;
  }

  renderEdit(
    doneAction: () => void,
    setBackAction: (action?: () => void) => void
  ) {
    return (
      <FilterableGroupEdit
        data={this.data}
        config={this.config}
        doneAction={doneAction}
        setBackAction={setBackAction}
      />
    );
  }

  renderInline() {
    return <FilterableGroupInline data={this.data} config={this.config} />;
  }

  displayDetails() {
    const decodedData = decodeData(this.data);

    const sel = decodedData.selected;
    if (sel.length > 0) {
      const title = selectionTitle(sel[0]);
      return {
        title: sel.length > 1 ? `${title} and ${sel.length - 1} more` : title,
        standaloneTitle: true,
        additionalText: sel.map((s) => selectionTitle(s)),
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

type PageData = {
  nodes: EntityNode[];
  pageMarker?: string;
  total: number;
  hintData?: HintData[];
};

type FilterableGroupEditProps = {
  data: string;
  config: configProto.FilterableGroup;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
};

function FilterableGroupEdit(props: FilterableGroupEditProps) {
  const underlaySource = useUnderlaySource();
  const updateEncodedCriteria = useUpdateCriteria();
  const updateCriteria = useCallback(
    (data: Data) => updateEncodedCriteria(encodeData(data)),
    [updateEncodedCriteria]
  );

  const decodedData = useMemo(() => decodeData(props.data), [props.data]);

  const [localCriteria, updateLocalCriteria] = useImmer(decodedData);

  const updateCriteriaFromLocal = useCallback(() => {
    updateCriteria(produce(decodedData, () => localCriteria));
  }, [updateCriteria, localCriteria]);

  const [searchState, updateSearchState] = useLocalSearchState<SearchState>();
  const [currentPage, setCurrentPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(25);

  const [filters, setFilters] = useState<ValueData[]>([]);
  const [selectedExclusion, setSelectedExclusion] = useState<
    string | undefined
  >();

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

  const selectedSet = useMemo(
    () =>
      new Set(
        localCriteria.selected.map((s) => s?.single?.key).filter(isValid)
      ),
    [localCriteria.selected]
  );

  useEffect(() => {
    // The extra function works around React defaulting to treating a function
    // as an update function.
    props.setBackAction(() => {
      if (props.data === encodeData(localCriteria)) {
        return undefined;
      } else {
        return unconfirmedChangesCallback;
      }
    });
  }, [searchState, localCriteria]);

  const attributes = useMemo(
    () => props.config.columns.map(({ key }) => key),
    [props.config.columns]
  );

  const entityGroup = underlaySource.lookupEntityGroup(
    props.config.entityGroup
  );

  const instancesState = useSWRInfinite(
    (pageIndex: number, prevPage?: PageData) => {
      if (prevPage && !prevPage.pageMarker) {
        return null;
      }

      return {
        type: "filterableGroupInstances",
        entityGroup: props.config.entityGroup,
        attributes,
        query: searchState?.query,
        filters: filters,
        pageMarker: prevPage?.pageMarker,
        rowsPerPage,
      };
    },
    async (key) => {
      if (!searchState?.query) {
        return { nodes: [], pageMarker: undefined };
      }

      const resP = underlaySource.searchEntityGroup(
        attributes,
        entityGroup.id,
        fromProtoSortOrder(props.config.sortOrder ?? DEFAULT_SORT_ORDER),
        {
          filters: generateFilters(searchState?.query ?? "", filters),
          pageSize: rowsPerPage,
          limit: 10000000,
          pageMarker: key.pageMarker,
        }
      );

      let hintDataP: Promise<HintData[]> | undefined;
      let hintData: HintData[] | undefined;
      if (!key.pageMarker) {
        hintDataP = underlaySource.getAllHintData(
          entityGroup.selectionEntity.name,
          {
            filter: makeBooleanLogicFilter(
              tanagra.BooleanLogicFilterOperatorEnum.And,
              generateFilters(searchState?.query ?? "")
            ),
          }
        );
      }

      const res = await resP;
      if (hintDataP) {
        hintData = await hintDataP;
      }

      return {
        nodes: res.nodes,
        pageMarker: res.pageMarker,
        total: res.total,
        hintData,
      };
    },
    {
      revalidateFirstPage: false,
    }
  );

  const columns: TreeGridColumn[] = useMemo(
    () => fromProtoColumns(props.config.columns),
    [props.config.columns]
  );

  const selectAllEnabled =
    instancesState?.data?.[0]?.total &&
    instancesState.data[0].total >= 25 &&
    instancesState.data[0].total <= 10000;

  return (
    <GridBox
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <GridLayout cols fillCol={0}>
        <GridLayout rows fillRow={1}>
          <GridBox
            sx={{
              px: 5,
              py: 3,
              height: "auto",
            }}
          >
            <Search
              placeholder="Search variants"
              disabled={!!selectedExclusion}
              onSearch={(query: string) => {
                updateSearchState((data: SearchState) => {
                  data.query = query;
                });
                setCurrentPage(0);
              }}
              initialValue={searchState?.query}
            />
          </GridBox>
          <Loading
            status={instancesState}
            isLoading={
              instancesState.isLoading || !instancesState.data?.[currentPage]
            }
            immediate
          >
            {instancesState.data?.length && instancesState.data?.[0]?.total ? (
              <GridLayout rows fillRow={1}>
                <GridLayout cols spacing={2} sx={{ px: 5 }}>
                  {!selectedExclusion ? (
                    <FilterButton
                      hintData={instancesState.data?.[0]?.hintData}
                      entity={entityGroup.selectionEntity.name}
                      config={props.config}
                      filters={filters}
                      setFilters={setFilters}
                    />
                  ) : null}
                  {!selectedExclusion ? (
                    <Tooltip
                      title={
                        !selectAllEnabled
                          ? "Number of results must be between 25 and 10,000 to Select all. For results less than 25, selections must be made individually."
                          : ""
                      }
                    >
                      <span>
                        <Button
                          variant="outlined"
                          disabled={!selectAllEnabled}
                          endIcon={<AddIcon />}
                          onClick={() =>
                            updateLocalCriteria((data) => {
                              data.selected.push({
                                id: generateId(),
                                all: {
                                  query: searchState?.query ?? "",
                                  values: filters,
                                  exclusions: [],
                                },
                              });
                            })
                          }
                        >
                          Select all
                        </Button>
                      </span>
                    </Tooltip>
                  ) : null}
                  {selectedExclusion ? (
                    <Button
                      variant="contained"
                      onClick={() => setSelectedExclusion(undefined)}
                    >
                      Confirm exclusions
                    </Button>
                  ) : null}
                </GridLayout>
                {instancesState.data[currentPage] ? (
                  <ResultsPage
                    columns={columns}
                    nodes={instancesState.data[currentPage].nodes}
                    selectedSet={selectedSet}
                    selectedExclusion={selectedExclusion}
                    localCriteria={localCriteria}
                    updateLocalCriteria={updateLocalCriteria}
                  />
                ) : null}
                <TablePagination
                  component="div"
                  count={instancesState.data?.[0]?.total ?? 0}
                  page={currentPage}
                  onPageChange={(e, page: number) => {
                    instancesState.setSize(
                      Math.max(instancesState.size, page + 1)
                    );
                    setCurrentPage(page);
                  }}
                  rowsPerPage={rowsPerPage}
                  onRowsPerPageChange={(e: ChangeEvent<HTMLInputElement>) => {
                    setRowsPerPage(Number(e.target.value));
                    setCurrentPage(0);
                  }}
                />
              </GridLayout>
            ) : (
              <Empty
                minHeight="300px"
                image={emptyImage}
                title={
                  searchState?.query?.length
                    ? "No matches found"
                    : "Enter a search query to start"
                }
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
          <GridLayout rows fillRow={0} spacing={1} width="320px">
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
                            key={s.id}
                            rows
                            sx={{
                              boxShadow:
                                i !== 0
                                  ? (theme) =>
                                      `0 -1px 0 ${theme.palette.divider}`
                                  : undefined,
                            }}
                          >
                            <GridLayout cols fillCol={0} rowAlign="middle">
                              <Typography variant="body2">
                                {selectionTitle(s)}
                              </Typography>
                              <IconButton
                                disabled={!!selectedExclusion}
                                onClick={() =>
                                  updateLocalCriteria((data) => {
                                    data.selected.splice(i, 1);
                                  })
                                }
                              >
                                <DeleteIcon />
                              </IconButton>
                            </GridLayout>
                            {s.all ? (
                              <SelectAllStats
                                config={props.config}
                                selectAll={s.all}
                                selected={s.id === selectedExclusion}
                                setSelected={(selected: boolean) => {
                                  const all = s.all;

                                  setSelectedExclusion(
                                    selected ? s.id : undefined
                                  );
                                  if (selected && all) {
                                    updateSearchState((data: SearchState) => {
                                      data.query = all.query;
                                    });

                                    setFilters(all.values);
                                  }
                                }}
                              />
                            ) : null}
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

type ResultsPageProps = {
  columns: TreeGridColumn[];
  nodes: EntityNode[];
  selectedSet: Set<DataKey>;
  selectedExclusion?: string;
  localCriteria: Data;
  updateLocalCriteria: (fn: (data: Data) => void) => void;
};

function ResultsPage(props: ResultsPageProps) {
  const data = useArrayAsTreeGridData(
    props.nodes.map((n) => n.data) ?? [],
    "key"
  );

  const selectAll = useMemo(() => {
    return props.localCriteria.selected.find(
      (s) => s.id === props.selectedExclusion
    )?.all;
  }, [props.localCriteria, props.selectedExclusion]);

  return (
    <TreeGrid
      columns={props.columns}
      data={data}
      rowCustomization={(key: TreeGridId, rowData: TreeGridRowData) => {
        const found = selectAll
          ? !!selectAll.exclusions.find((e) => e.key === key)
          : props.selectedSet.has(key);
        const newSelection = {
          key,
          name: String(rowData[props.columns[0].key] ?? "Unknown"),
        };

        return [
          {
            column: 0,
            prefixElements: selectAll ? (
              <IconButton
                color={!found ? "error" : undefined}
                sx={found ? containedIconButtonSx("error") : undefined}
                onClick={() => {
                  props.updateLocalCriteria((data) => {
                    const all = data.selected.find(
                      (s) => s.id === props.selectedExclusion
                    )?.all;
                    if (all) {
                      if (found) {
                        all.exclusions = all.exclusions.filter(
                          (e) => e.key !== key
                        );
                      } else {
                        all.exclusions.push(newSelection);
                      }
                    }
                  });
                }}
              >
                <ClearIcon fontSize="small" />
              </IconButton>
            ) : (
              <Checkbox
                size="small"
                fontSize="inherit"
                checked={found}
                onChange={() => {
                  props.updateLocalCriteria((data) => {
                    if (found) {
                      data.selected = data.selected.filter(
                        (s) => s.single?.key !== key
                      );
                    } else {
                      data.selected.push({
                        id: generateId(),
                        single: newSelection,
                      });
                    }
                  });
                }}
              />
            ),
          },
        ];
      }}
    />
  );
}

type FilterButtonProps = {
  hintData?: HintData[];
  entity: string;
  config: configProto.FilterableGroup;
  filters: ValueData[];
  setFilters: (valueData: ValueData[]) => void;
};

function FilterButton(props: FilterButtonProps) {
  const [filters, setFilters] = useState<ValueData[] | undefined>();
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

  const open = !!anchorEl;
  const id = open ? "filterableGroup-filters" : undefined;

  const onClose = () => {
    setAnchorEl(null);
    if (isValid(filters)) {
      props.setFilters(filters);
    }
  };

  return (
    <>
      <Button
        aria-describedby={id}
        variant="contained"
        disabled={!props.hintData?.length}
        endIcon={<FilterListIcon />}
        onClick={(event: MouseEvent<HTMLButtonElement>) =>
          setAnchorEl(event.currentTarget)
        }
      >
        Filters
      </Button>
      <Popover
        id={id}
        open={open}
        anchorEl={anchorEl}
        onClose={() => onClose()}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left",
        }}
      >
        <GridBox sx={{ p: 2, width: "500px" }}>
          <GridLayout rows spacing={2} height="auto">
            <ValueDataEdit
              hintEntity={props.entity}
              hintData={props.hintData}
              valueConfigs={props.config.valueConfigs}
              valueData={filters ?? props.filters}
              update={(filters) => setFilters(filters ?? [])}
            />
            <GridLayout cols colAlign="right">
              <Button variant="contained" onClick={() => onClose()}>
                Apply
              </Button>
            </GridLayout>
          </GridLayout>
        </GridBox>
      </Popover>
    </>
  );
}

type SelectAllStatsProps = {
  config: configProto.FilterableGroup;
  selectAll: SelectAll;
  selected?: boolean;
  setSelected?: (selected: boolean) => void;
};

function SelectAllStats(props: SelectAllStatsProps) {
  return (
    <GridLayout rows sx={{ pl: 2 }}>
      <Typography variant="body2em">
        Search terms:&nbsp;
        <Typography variant="body2" component="span">
          {props.selectAll.query}
        </Typography>
      </Typography>
      {props.selectAll.values.map((v) => {
        const name =
          props.config.valueConfigs.find((c) => c.attribute === v.attribute)
            ?.title ?? "Unknown";
        const value = v.numeric
          ? `${v.range.min}-${v.range.max}`
          : v.selected.map((s) => s.name).join(", ");
        return (
          <Typography key={v.attribute} variant="body2em">
            {name + ": "}
            <Typography variant="body2" component="span">
              {value}
            </Typography>
          </Typography>
        );
      })}
      <GridLayout cols rowAlign="middle">
        {props.setSelected ? (
          <IconButton
            sx={props.selected ? containedIconButtonSx("primary") : undefined}
            onClick={() => {
              if (props.setSelected) {
                props.setSelected(!props.selected);
              }
            }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
        ) : null}
        <Typography variant="body2em">
          {"Exclusions: "}
          <Typography variant="body2" component="span">
            {props.selectAll.exclusions.length
              ? props.selectAll.exclusions.map((e) => e.name).join(", ")
              : "None"}
          </Typography>
        </Typography>
      </GridLayout>
    </GridLayout>
  );
}

type FilterableGroupInlineProps = {
  data: string;
  config: configProto.FilterableGroup;
};

function FilterableGroupInline(props: FilterableGroupInlineProps) {
  const decodedData = useMemo(() => decodeData(props.data), [props.data]);
  if (!decodedData.selected.length) {
    return null;
  }

  return (
    <GridLayout rows height="auto">
      {decodedData.selected.map((s, i) => (
        <GridLayout
          key={s.id}
          rows
          sx={{
            boxShadow:
              i !== 0
                ? (theme) => `0 -1px 0 ${theme.palette.divider}`
                : undefined,
          }}
        >
          <Typography variant="body2">{selectionTitle(s)}</Typography>
          {s.all ? (
            <SelectAllStats config={props.config} selectAll={s.all} />
          ) : null}
        </GridLayout>
      ))}
    </GridLayout>
  );
}

function decodeData(data: string): Data {
  const message =
    data[0] === "{"
      ? dataProto.FilterableGroup.fromJSON(JSON.parse(data))
      : dataProto.FilterableGroup.decode(base64ToBytes(data));

  return {
    selected:
      message.selected?.map((s) => {
        if (!s.single && !s.all) {
          throw new Error(
            `No selection defined in ${JSON.stringify(message)}.`
          );
        }

        return {
          id: s.id,
          single: s.single
            ? {
                key: dataKeyFromProto(s.single.key),
                name: s.single.name,
              }
            : undefined,
          all: s.all
            ? {
                query: s.all.query,
                values: s.all.values.map((v) => decodeValueData(v)) ?? [],
                exclusions:
                  s.all.exclusions.map((e) => ({
                    key: dataKeyFromProto(e.key),
                    name: e.name,
                  })) ?? [],
              }
            : undefined,
        };
      }) ?? [],
  };
}

function encodeData(data: Data): string {
  const message: dataProto.FilterableGroup = {
    selected: data.selected.map((s) => ({
      id: s.id,
      single: s.single
        ? {
            key: protoFromDataKey(s.single.key),
            name: s.single.name,
          }
        : undefined,
      all: s.all
        ? {
            query: s.all.query,
            values: s.all.values.map((v) => encodeValueData(v)),
            exclusions: s.all.exclusions.map((e) => ({
              key: protoFromDataKey(e.key),
              name: e.name,
            })),
          }
        : undefined,
    })),
  };
  return JSON.stringify(dataProto.FilterableGroup.toJSON(message));
}

function decodeConfig(
  selector: CommonSelectorConfig
): configProto.FilterableGroup {
  return configProto.FilterableGroup.fromJSON(
    JSON.parse(selector.pluginConfig)
  );
}

const rsRE = /rs\d+/;
const variantIdRE = /\d+-\d+-\w+-\w+/;

function generateFilters(
  query: string,
  filters?: ValueData[]
): tanagra.Filter[] {
  const operands: tanagra.Filter[] = [];

  if (query !== "") {
    // TODO(BENCH-4370): Consider how to make this configurable.
    if (rsRE.test(query)) {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: "rs_number",
            operator: tanagra.AttributeFilterOperatorEnum.In,
            values: [literalFromDataValue(query)],
          },
        },
      });
    } else if (variantIdRE.test(query)) {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: "variant_id",
            operator: tanagra.AttributeFilterOperatorEnum.Equals,
            values: [literalFromDataValue(query)],
          },
        },
      });
    } else {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Text,
        filterUnion: {
          textFilter: {
            matchType: tanagra.TextFilterMatchTypeEnum.ExactMatch,
            text: query,
            attribute: "gene",
          },
        },
      });
    }
  }

  filters?.forEach((vd) => {
    if (vd.numeric) {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: vd.attribute,
            operator: tanagra.AttributeFilterOperatorEnum.Between,
            values: [
              literalFromDataValue(vd.range.min),
              literalFromDataValue(vd.range.max),
            ],
          },
        },
      });
    } else {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: vd.attribute,
            operator: tanagra.AttributeFilterOperatorEnum.In,
            values: vd.selected.map((s) => literalFromDataValue(s.value)),
          },
        },
      });
    }
  });

  return operands;
}
