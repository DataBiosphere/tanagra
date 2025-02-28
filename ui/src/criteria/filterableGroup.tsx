import { ValueDataEdit } from "criteria/valueDataEdit";
import AddIcon from "@mui/icons-material/Add";
import ClearIcon from "@mui/icons-material/Clear";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import FilterListIcon from "@mui/icons-material/FilterList";
import InfoIcon from "@mui/icons-material/Info";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Popover from "@mui/material/Popover";
import TablePagination from "@mui/material/TablePagination";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import {
  createCriteria,
  CriteriaPlugin,
  generateId,
  newCohort,
  registerCriteriaPlugin,
} from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import { containedIconButtonSx } from "components/iconButton";
import Loading from "components/loading";
import { Search } from "components/search";
import { useSimpleDialog } from "components/simpleDialog";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridId,
  TreeGridRowData,
} from "components/treeGrid";
import { fromProtoColumns } from "components/treeGridHelpers";
import { useArrayAsTreeGridData } from "components/treeGridHelpers";
import {
  decodeValueData,
  encodeValueData,
  ValueData,
} from "criteria/valueData";
import {
  DEFAULT_SORT_ORDER,
  fromProtoSortOrder,
  ITEM_COUNT_ATTRIBUTE,
  SortDirection,
} from "data/configuration";
import {
  Cohort,
  CommonSelectorConfig,
  dataKeyFromProto,
  EntityNode,
  FilterCountValue,
  HintData,
  literalFromDataValue,
  makeBooleanLogicFilter,
  protoFromDataKey,
  UnderlaySource,
} from "data/source";
import {
  convertAttributeStringToDataValue,
  DataEntry,
  DataKey,
} from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUpdateCriteria } from "hooks";
import emptyImage from "images/empty.svg";
import { produce } from "immer";
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
import useSWRImmutable from "swr/immutable";
import useSWRInfinite from "swr/infinite";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { base64ToBytes } from "util/base64";
import { useLocalSearchState } from "util/searchState";
import { isValid } from "util/valid";
import { processFilterCountValues } from "viz/viz";

type SingleSelect = {
  key: DataKey;
  name: string;
};

type SelectAll = {
  query: string;
  valueData: ValueData[];
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
@registerCriteriaPlugin(
  "filterableGroup",
  (
    underlaySource: UnderlaySource,
    c: CommonSelectorConfig,
    dataEntries?: DataEntry[]
  ) => {
    if (dataEntries?.length && dataEntries[0].encoded) {
      return String(dataEntries[0].encoded);
    }

    return encodeData({
      selected: [],
    });
  }
)
class _ implements CriteriaPlugin<string> {
  public data: string;
  private selector: CommonSelectorConfig;
  private config: configProto.FilterableGroup;

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
      <FilterableGroupEdit
        data={this.data}
        config={this.config}
        doneAction={doneAction}
        setBackAction={setBackAction}
        selector={this.selector}
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
  invalidQuery?: boolean;
};

type FilterableGroupEditProps = {
  data: string;
  config: configProto.FilterableGroup;
  doneAction: () => void;
  setBackAction: (action?: () => void) => void;
  selector: CommonSelectorConfig;
};

export function FilterableGroupEdit(props: FilterableGroupEditProps) {
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
  }, [updateCriteria, localCriteria, decodedData]);

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
  }, [searchState, localCriteria, props, unconfirmedChangesCallback]);

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
      const generatedFilters = generateFilters(
        underlaySource,
        props.config,
        searchState?.query ?? ""
      );

      if (!searchState?.query || !generatedFilters.length) {
        return {
          nodes: [],
          pageMarker: undefined,
          invalidQuery: !generatedFilters.length,
        };
      }

      const resP = underlaySource.searchEntityGroup(
        attributes,
        entityGroup.id,
        fromProtoSortOrder(props.config.sortOrder ?? DEFAULT_SORT_ORDER),
        {
          filters: generateFilters(
            underlaySource,
            props.config,
            searchState?.query ?? "",
            filters
          ),
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
              generatedFilters
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
            <GridLayout cols fillCol={0} spacing={1} rowAlign="middle">
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
              {props.config.searchConfigs.length ? (
                <Tooltip
                  title={
                    <GridLayout rows>
                      <Typography variant="body1">Query examples:</Typography>
                      {[...props.config.searchConfigs]
                        .sort((a, b) =>
                          Number((a.displayOrder ?? 0) - (b.displayOrder ?? 0))
                        )
                        .map((sc) => (
                          <Typography key={sc.name} variant="body2em">
                            {sc.name}:&nbsp;
                            <Typography variant="body2" component="span">
                              {sc.example}
                            </Typography>
                          </Typography>
                        ))}
                    </GridLayout>
                  }
                >
                  <InfoIcon color="primary" sx={{ display: "block" }} />
                </Tooltip>
              ) : null}
            </GridLayout>
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
                                  valueData: filters,
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
                  instancesState.data?.[0]?.invalidQuery
                    ? "Invalid query format"
                    : searchState?.query?.length
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
                                size="small"
                              >
                                <DeleteIcon />
                              </IconButton>
                            </GridLayout>
                            {s.all ? (
                              <SelectAllStats
                                config={props.config}
                                selectAll={s.all}
                                selector={props.selector}
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

                                    setFilters(all.valueData);
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

export function ResultsPage(props: ResultsPageProps) {
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
                size="small"
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

export function FilterButton(props: FilterButtonProps) {
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
  selector?: CommonSelectorConfig;
  selected?: boolean;
  setSelected?: (selected: boolean) => void;
};

export function SelectAllStats(props: SelectAllStatsProps) {
  const underlaySource = useUnderlaySource();

  const vizDataConfig = {
    sources: [
      {
        criteriaSelector: "unused",
        joins: [],
        attributes: [
          {
            attribute: ITEM_COUNT_ATTRIBUTE,
            numericBucketing: {
              thresholds: [1000, 5000, 10000, 50000, 100000, 500000],
              includeLesser: true,
              includeGreater: true,
            },
          },
        ],
      },
    ],
  };

  const cohort = useSelectAllCohort(
    underlaySource,
    props.selectAll,
    props.selector
  );

  const statsState = useSWRImmutable(
    { type: "variantsVizData", cohort, selectAll: props.selectAll },
    async () => {
      if (!cohort) {
        return {
          variants: [],
          total: 0,
          participants: 0,
        };
      }

      // TODO(tjennison): Ideally this would be fully shared with the viz code
      // but there's currently no way to group by relationship fields (i.e.
      // ITEM_COUNT_ATTRIBUTE) using the cohortCount API. Adding them to the
      // input wouldn't be too hard but the output would need signficant
      // changes.
      const instancesP = await underlaySource.searchEntityGroup(
        vizDataConfig.sources[0].attributes.map((a) => a.attribute),
        props.config.entityGroup,
        {
          attribute: ITEM_COUNT_ATTRIBUTE,
          direction: SortDirection.Desc,
        },
        {
          filters: generateFilters(
            underlaySource,
            props.config,
            props.selectAll.query,
            props.selectAll.valueData
          ),
          limit: 1000000,
          pageSize: 1000000,
        }
      );

      const participantsP = underlaySource.criteriaCount(cohort.groupSections);

      const instances = (await instancesP) ?? [];
      const fcvs: FilterCountValue[] = [];
      instances.nodes.forEach((i) =>
        fcvs.push({
          ...i.data,
          count: 1,
        })
      );
      const variants = processFilterCountValues(vizDataConfig, fcvs);

      const participants = (await participantsP)?.[0]?.count ?? 0;

      return {
        variants,
        total: variants.reduce((tot, v) => (v.values[0].numeric ?? 0) + tot, 0),
        participants,
      };
    }
  );

  return (
    <GridLayout rows sx={{ pl: 2 }}>
      <Typography variant="body2em">
        Search terms:&nbsp;
        <Typography variant="body2" component="span">
          {props.selectAll.query}
        </Typography>
      </Typography>
      {props.selectAll.valueData.map((v) => {
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
            size="small"
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
      {props.selector ? (
        <GridLayout rows>
          <Typography variant="body2em">Participant overview:</Typography>
          <Loading status={statsState}>
            <GridLayout rows sx={{ pl: 2 }}>
              <Typography variant="body2em">
                {"Total participant count: "}
                <Typography variant="body2" component="span">
                  {statsState.data?.participants}
                </Typography>
              </Typography>
              <Typography variant="body2em">
                {"Total variant count: "}
                <Typography variant="body2" component="span">
                  {statsState.data?.total}
                </Typography>
              </Typography>
              <GridLayout rows sx={{ pl: 2 }}>
                {statsState.data?.variants?.map((v) => (
                  <Typography key={v.keys[0].name} variant="body2em">
                    {`${v.keys[0].name} participants: `}
                    <Typography variant="body2" component="span">
                      {v.values[0].numeric}
                    </Typography>
                  </Typography>
                ))}
              </GridLayout>
            </GridLayout>
          </Loading>
        </GridLayout>
      ) : null}
    </GridLayout>
  );
}

function useSelectAllCohort(
  underlaySource: UnderlaySource,
  selectAll: SelectAll,
  selector?: CommonSelectorConfig
): Cohort | undefined {
  return useMemo(() => {
    if (!selector) {
      return undefined;
    }

    return newCohort(
      underlaySource.underlay.name,
      createCriteria(underlaySource, selector, [
        {
          key: generateId(),
          encoded: encodeData({
            selected: [
              {
                id: generateId(),
                all: selectAll,
              },
            ],
          }),
        },
      ])
    );
  }, [selectAll, selector, underlaySource]);
}

type FilterableGroupInlineProps = {
  data: string;
  config: configProto.FilterableGroup;
};

export function FilterableGroupInline(props: FilterableGroupInlineProps) {
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
                valueData: s.all.valueData.map((v) => decodeValueData(v)) ?? [],
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
            valueData: s.all.valueData.map((v) => encodeValueData(v)),
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

const operatorMap = {
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator.UNRECOGNIZED]:
    undefined,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator
    .OPERATOR_UNKNOWN]: tanagra.AttributeFilterOperatorEnum.Equals,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator.OPERATOR_EQUALS]:
    tanagra.AttributeFilterOperatorEnum.Equals,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator
    .OPERATOR_GREATER_THAN]: tanagra.AttributeFilterOperatorEnum.GreaterThan,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator
    .OPERATOR_GREATER_THAN_OR_EQUAL]:
    tanagra.AttributeFilterOperatorEnum.GreaterThanOrEqual,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator
    .OPERATOR_LESS_THAN]: tanagra.AttributeFilterOperatorEnum.LessThan,
  [configProto.FilterableGroup_SearchConfig_Parameter_Operator
    .OPERATOR_LESS_THAN_OR_EQUAL]:
    tanagra.AttributeFilterOperatorEnum.LessThanOrEqual,
};

function generateFilters(
  underlaySource: UnderlaySource,
  config: configProto.FilterableGroup,
  query: string,
  filters?: ValueData[]
): tanagra.Filter[] {
  const [entity] = underlaySource.lookupRelatedEntity(config.entityGroup);
  const operands: (tanagra.Filter | null)[] = [];

  if (query !== "") {
    if (config.searchConfigs.length > 0) {
      for (const sc of config.searchConfigs) {
        const match = query.match(new RegExp(sc.regex));
        if (!match || match[0].length !== query.length) {
          continue;
        }

        sc.parameters.forEach((p, i) => {
          let group = match.length > 1 ? match[i + 1] : match[0];
          if (
            p.case ===
            configProto.FilterableGroup_SearchConfig_Parameter_Case.CASE_UPPER
          ) {
            group = group.toUpperCase();
          } else if (
            p.case ===
            configProto.FilterableGroup_SearchConfig_Parameter_Case.CASE_LOWER
          ) {
            group = group.toLowerCase();
          }

          const attribute = entity.attributes.find(
            (a) => a.name === p.attribute
          );
          if (!attribute) {
            throw new Error(
              `Unknown attribute "${p.attribute}" in entity "${entity.name}".`
            );
          }
          const value = convertAttributeStringToDataValue(group, attribute);

          const operator = operatorMap[p.operator];
          if (!operator) {
            throw new Error(`Unknown operator in ${JSON.stringify(p)}.`);
          }

          operands.push({
            filterType: tanagra.FilterFilterTypeEnum.Attribute,
            filterUnion: {
              attributeFilter: {
                attribute: p.attribute,
                operator,
                values: [literalFromDataValue(value)],
              },
            },
          });
        });
        break;
      }

      if (!operands.length) {
        return [];
      }
    } else {
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Text,
        filterUnion: {
          textFilter: {
            matchType: tanagra.TextFilterMatchTypeEnum.ExactMatch,
            text: query,
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
      const attribute = entity.attributes.find((a) => a.name === vd.attribute);
      if (!attribute) {
        throw new Error(
          `Unknown attribute "${vd.attribute}" in entity "${entity.name}".`
        );
      }

      const filtersToOr: tanagra.Filter[] = [];
      const enumLiterals: tanagra.Literal[] = [];
      vd.selected.forEach((s) => {
        if (s.value === attribute.emptyValueDisplay) {
          // empty value is selected: entries with no value for this attribute
          filtersToOr.push({
            filterType: tanagra.FilterFilterTypeEnum.Attribute,
            filterUnion: {
              attributeFilter: {
                attribute: vd.attribute,
                operator: tanagra.AttributeFilterOperatorEnum.IsNull,
              },
            },
          });
        } else {
          enumLiterals.push(literalFromDataValue(s.value));
        }
      });

      if (enumLiterals.length > 0) {
        filtersToOr.push({
          filterType: tanagra.FilterFilterTypeEnum.Attribute,
          filterUnion: {
            attributeFilter: {
              attribute: vd.attribute,
              operator:
                enumLiterals.length == 1
                  ? tanagra.AttributeFilterOperatorEnum.Equals
                  : tanagra.AttributeFilterOperatorEnum.In,
              values: enumLiterals,
            },
          },
        });
      }

      operands.push(
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterOperatorEnum.Or,
          filtersToOr
        )
      );
    }
  });

  return operands.filter(isValid);
}
