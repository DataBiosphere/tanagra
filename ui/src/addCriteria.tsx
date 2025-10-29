import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  createCriteria,
  getCriteriaPlugin,
  searchCriteria,
  sectionName,
} from "cohort";
import { insertCohortCriteria, useCohortContext } from "cohortContext";
import Empty from "components/empty";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridId,
  TreeGridItem,
  TreeGridData,
} from "components/treeGrid";
import { MergedItem } from "data/mergeLists";
import { Criteria, isTemporalSection } from "data/source";
import { DataEntry, DataKey } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import {
  insertFeatureSetCriteria,
  insertPredefinedFeatureSetCriteria,
  useFeatureSetContext,
} from "featureSet/featureSetContext";
import {
  useCohortGroupSectionAndGroup,
  useFeatureSet,
  useUnderlay,
} from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useMemo } from "react";
import {
  addCohortCriteriaURL,
  addFeatureSetCriteriaURL,
  addByCodeURL,
  cohortURL,
  featureSetURL,
  newCriteriaURL,
  useIsSecondBlock,
} from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagraUnderlay from "tanagra-underlay/underlayConfig";
import { safeRegExp } from "util/safeRegExp";
import {
  useGlobalSearchState,
  useLocalSearchState,
  useNavigate,
} from "util/searchState";
import { isValid } from "util/valid";

type LocalSearchState = {
  search?: string;
};

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, section, sectionIndex } = useCohortGroupSectionAndGroup();
  const secondBlock = useIsSecondBlock();

  const onInsertCriteria = useCallback(
    (criteria: Criteria) => {
      const group = insertCohortCriteria(
        context,
        section.id,
        criteria,
        secondBlock
      );
      navigate("../../" + cohortURL(cohort.id, section.id, group.id));
    },
    [context, cohort.id, section.id, navigate, secondBlock]
  );

  const name = sectionName(section, sectionIndex);

  return (
    <AddCriteria
      title={`Adding criteria for ${name}`}
      onInsertCriteria={onInsertCriteria}
      temporal={isTemporalSection(section)}
    />
  );
}

export function AddFeatureSetCriteria() {
  const navigate = useNavigate();
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();

  const onInsertCriteria = useCallback(
    (criteria: Criteria) => {
      insertFeatureSetCriteria(context, criteria);
      navigate("../../" + featureSetURL(featureSet.id));
    },
    [context, featureSet.id, navigate]
  );

  const onInsertPredefinedCriteria = useCallback(
    (criteria: string, title: string) => {
      insertPredefinedFeatureSetCriteria(context, criteria, title);
      navigate("../../" + featureSetURL(featureSet.id));
    },
    [context, featureSet.id, navigate]
  );

  return (
    <AddCriteria
      featureSet
      title={`Adding criteria for ${featureSet.name}`}
      onInsertCriteria={onInsertCriteria}
      excludedPredefinedCriteria={featureSet.predefinedCriteria}
      onInsertPredefinedCriteria={onInsertPredefinedCriteria}
    />
  );
}

type AddCriteriaOption = {
  name: string;
  title: string;
  showMore: boolean;
  category?: string;
  tags?: string[];
  cohort?: boolean;
  featureSet?: boolean;
  selector?: tanagraUnderlay.SZCriteriaSelector;
  fn?: () => void;
};

type AddCriteriaProps = {
  featureSet?: boolean;
  title: string;
  backAction?: () => void;
  onInsertCriteria: (criteria: Criteria) => void;
  excludedPredefinedCriteria?: string[];
  onInsertPredefinedCriteria?: (criteria: string, title: string) => void;
  temporal?: boolean;
};

function AddCriteria(props: AddCriteriaProps) {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const navigate = useNavigate();

  const query = useLocalSearchState<LocalSearchState>()[0].search ?? "";
  const [globalSearchState, updateGlobalSearchState] = useGlobalSearchState();

  const selectors = underlay.criteriaSelectors;
  const predefinedCriteria = underlay.prepackagedDataFeatures;
  const searchConfig = underlay.uiConfiguration.criteriaSearchConfig;

  const options = useMemo(() => {
    const options: AddCriteriaOption[] = [];
    if (props.onInsertPredefinedCriteria) {
      predefinedCriteria.forEach((p) =>
        options.push({
          name: p.name,
          title: p.displayName,
          category: "Predefined",
          featureSet: true,
          showMore: false,
        })
      );
    }

    selectors.forEach((s) =>
      options.push({
        name: s.name,
        title: s.displayName,
        category: s.display.category,
        tags: s.display.tags,
        selector: s,
        cohort: s.isEnabledForCohorts,
        featureSet: s.isEnabledForDataFeatureSets,
        showMore: !!getCriteriaPlugin(createCriteria(underlaySource, s))
          .renderEdit,
      })
    );

    options.push({
      name: "tAddFeatureSet",
      title: "Add feature set criteria",
      category: "Other",
      tags: [],
      cohort: !underlay.uiConfiguration.featureConfig?.disableFeatureSets,
      showMore: false,
      fn: () => {
        navigate(addFeatureSetCriteriaURL());
      },
    });

    options.push({
      name: "tAddCohort",
      title: "Add cohort criteria",
      category: "Other",
      tags: [],
      featureSet: true,
      showMore: false,
      fn: () => {
        navigate(addCohortCriteriaURL());
      },
    });

    if (underlay.uiConfiguration.featureConfig?.enableAddByCode) {
      options.push({
        name: "tAddByCode",
        title: "Add criteria by code",
        category: "Other",
        tags: [],
        cohort: true,
        showMore: false,
        fn: () => {
          navigate(addByCodeURL());
        },
      });
    }

    return options;
  }, [
    props.onInsertPredefinedCriteria,
    selectors,
    predefinedCriteria,
    navigate,
    underlay.uiConfiguration,
    underlaySource,
  ]);

  const tagList = useMemo(
    () =>
      options.reduce((out: string[], o) => {
        o.tags?.forEach((t) => {
          if (out.indexOf(t) < 0) {
            out.push(t);
          }
        });
        return out;
      }, []),
    [options]
  );

  const optionsMap = useMemo(
    () => new Map(options.map((o) => [o.name, o])),
    [options]
  );

  const selectedTags = useMemo(
    () => new Set(globalSearchState.addCriteriaTags ?? [tagList[0]]),
    [globalSearchState.addCriteriaTags, tagList]
  );

  const selectedOptions = useMemo(() => {
    const featureSet = props.featureSet;
    const temporal = props.temporal;
    return options.filter((option) => {
      if (
        (featureSet && !option.featureSet) ||
        (!featureSet && !option.cohort)
      ) {
        return false;
      }

      if (temporal && !option.selector?.supportsTemporalQueries) {
        return false;
      }

      if (
        selectedTags.size &&
        option.tags?.length &&
        !option.tags?.reduce((out, t) => out || selectedTags.has(t), false)
      ) {
        return false;
      }

      return true;
    });
  }, [selectedTags, options, props.featureSet, props.temporal]);

  const categories = useMemo(() => {
    const categories: AddCriteriaOption[][] = [];
    const [re] = safeRegExp(query);

    for (const option of selectedOptions) {
      if (query && option.title.search(re) < 0) {
        continue;
      }

      let category: AddCriteriaOption[] | undefined;
      for (const c of categories) {
        if (c[0].category === option.category) {
          category = c;
          break;
        }
      }

      if (category) {
        category.push(option);
      } else {
        categories.push([option]);
      }
    }

    for (const c of categories) {
      c.sort((a, b) => (b.showMore ? 1 : 0) - (a.showMore ? 1 : 0));
    }

    return categories;
  }, [query, selectedOptions]);

  const columns = useMemo(
    () => [
      {
        key: "t_criteria_type",
        width: searchConfig.criteriaTypeWidth,
        title: "Criteria type",
      },
      ...searchConfig.columns,
      {
        key: "t_add_button",
        width: 80,
      },
    ],
    [searchConfig]
  );

  const onClick = useCallback(
    (option: AddCriteriaOption, dataEntry?: DataEntry) => {
      const onInsertCriteria = props.onInsertCriteria;
      const onInsertPredefinedCriteria = props.onInsertPredefinedCriteria;
      if (option.fn) {
        option.fn();
      } else if (option.selector) {
        const criteria = createCriteria(
          underlaySource,
          option.selector,
          dataEntry ? [dataEntry] : undefined
        );
        if (!!getCriteriaPlugin(criteria).renderEdit && !dataEntry) {
          navigate(newCriteriaURL(option.name));
        } else {
          onInsertCriteria(criteria);
        }
      } else {
        onInsertPredefinedCriteria?.(option.name, option.title);
      }
    },
    [
      underlaySource,
      navigate,
      props.onInsertCriteria,
      props.onInsertPredefinedCriteria,
    ]
  );

  const search = useCallback(async () => {
    const children: DataKey[] = [];
    const rows = new Map<TreeGridId, CriteriaItem>();

    if (query) {
      const res = await searchCriteria(
        underlaySource,
        selectedOptions.map((o) => o.selector).filter(isValid),
        query
      );

      res.data.forEach((entry) => {
        const key = `${entry.source}~${entry.data.key}`;
        children.push(key);

        const item: CriteriaItem = {
          data: {
            ...entry.data,
            t_criteria_type: (
              <Chip label={optionsMap.get(entry.source)?.title} size="small" />
            ),
          },
          entry: entry,
        };
        rows.set(key, item);
      });
    }

    return {
      rows,
      children,
    };
  }, [underlaySource, query, selectedOptions, optionsMap]);
  const searchState = useSWRImmutable<TreeGridData<CriteriaItem>>(
    {
      component: "AddCriteria",
      underlayName: underlay.name,
      query,
      selectedTags,
    },
    search
  );

  return (
    <GridLayout rows>
      <ActionBar title={props.title} backAction={props.backAction} />
      <GridLayout rows spacing={2} sx={{ px: 5, py: 2 }}>
        <GridBox>
          <Search
            placeholder="Search by code or description"
            initialValue={query}
          />
        </GridBox>
        <GridLayout cols spacing={1} rowAlign="baseline">
          {tagList.length > 0 ? (
            <GridLayout cols spacing={1} rowAlign="baseline">
              <Typography variant="body1">Showing criteria in:</Typography>
              <FormControl variant="outlined">
                <Select
                  variant="outlined"
                  multiple
                  displayEmpty
                  value={tagList.filter((t) => selectedTags.has(t))}
                  renderValue={(selected) =>
                    selected?.length ? (
                      <Typography variant="body1">
                        {selected.join(", ")}
                      </Typography>
                    ) : (
                      <Typography variant="body1" component="em">
                        Any
                      </Typography>
                    )
                  }
                  onChange={(event: SelectChangeEvent<string[]>) => {
                    updateGlobalSearchState((state) => {
                      state.addCriteriaTags = event.target.value as string[];
                    });
                  }}
                  sx={{
                    color: (theme) => theme.palette.primary.main,
                    "& .MuiOutlinedInput-input": {
                      py: "2px",
                    },
                    "& .MuiOutlinedInput-notchedOutline": {
                      borderColor: (theme) => theme.palette.primary.main,
                    },
                  }}
                >
                  {tagList.map((t) => (
                    <MenuItem key={t} value={t}>
                      {t}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Typography variant="body1">.</Typography>
            </GridLayout>
          ) : null}
          {query === "" && <GridLayout cols spacing={0.5} rowAlign="middle">
            <Typography variant="body1">Use</Typography>
            <AddIcon
              sx={{
                display: "grid",
                color: (theme) => theme.palette.primary.main,
              }}
            />
            <Typography variant="body1">to directly add and</Typography>
            <SearchIcon
              sx={{
                display: "grid",
                color: (theme) => theme.palette.primary.main,
              }}
            />
            <Typography variant="body1">to explore.</Typography>
          </GridLayout> }
        </GridLayout>
        {props.temporal ? (
          <Typography variant="body1">
            Some criteria are ineligible for temporal comparisons.
          </Typography>
        ) : null}
        <GridLayout rows spacing={2} sx={{ height: "auto" }}>
          {categories.map((category) => (
            <GridLayout key={category[0].category} rows spacing={2}>
              <Typography variant="h6">{category[0].category}</Typography>
              <GridBox
                sx={{
                  display: "flex",
                  flexWrap: "wrap",
                  alignItems: "flex-start",
                  gap: 2,
                }}
              >
                {category.flatMap((option, i) => {
                  const disabled =
                    (props.excludedPredefinedCriteria?.indexOf(option.name) ??
                      -1) >= 0;
                  const button = (
                    <Button
                      key={option.name}
                      data-testid={option.name}
                      variant="outlined"
                      startIcon={option.showMore ? <SearchIcon /> : <AddIcon />}
                      disabled={disabled}
                      title={
                        disabled
                          ? "Only one copy of this feature may be added"
                          : undefined
                      }
                      sx={{ "&.Mui-disabled": { pointerEvents: "auto" } }}
                      onClick={() => onClick(option)}
                    >
                      {option.title}
                    </Button>
                  );

                  if (i > 0 && category[i - 1].showMore != option.showMore) {
                    return [
                      <Divider
                        key="t_divider"
                        orientation="vertical"
                        flexItem
                      />,
                      button,
                    ];
                  }
                  return button;
                })}
              </GridBox>
            </GridLayout>
          ))}
          {query ? <Divider /> : null}
        </GridLayout>
        {query ? (
          <Paper>
            <Loading status={searchState}>
              {!searchState.data?.children?.length ? (
                <Empty
                  minHeight="300px"
                  image={emptyImage}
                  title="No matches found"
                />
              ) : (
                <TreeGrid
                  columns={columns}
                  data={searchState.data}
                  rowCustomization={(id, item) => {
                    const option = optionsMap.get(item.entry?.source ?? "");
                    if (!option || !item?.entry?.source) {
                      throw new Error(
                        `Item source "${item?.entry?.source}" doesn't match any criteria config ID.`
                      );
                    }

                    return [
                      {
                        column: columns.length - 1,
                        content: (
                          <GridLayout colAlign="center">
                            <Button
                              data-testid={
                                item.data[searchConfig.columns[0].key]
                              }
                              onClick={() => onClick(option, item.entry?.data)}
                              variant="outlined"
                            >
                              Add
                            </Button>
                          </GridLayout>
                        ),
                      },
                    ];
                  }}
                />
              )}
            </Loading>
          </Paper>
        ) : null}
      </GridLayout>
    </GridLayout>
  );
}

type CriteriaItem = TreeGridItem & {
  entry?: MergedItem<DataEntry>;
};
