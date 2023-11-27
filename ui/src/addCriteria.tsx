import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { insertCohortCriteria, useCohortContext } from "cohortContext";
import Empty from "components/empty";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
  TreeGridRowData,
} from "components/treegrid";
import { MergedItem } from "data/mergeLists";
import { useSource } from "data/sourceContext";
import { DataEntry, DataKey } from "data/types";
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
import { cohortURL, featureSetURL, newCriteriaURL } from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagraUI from "tanagra-ui";
import { CriteriaConfig } from "underlaysSlice";
import { safeRegExp } from "util/safeRegExp";
import {
  useGlobalSearchState,
  useLocalSearchState,
  useNavigate,
} from "util/searchState";
import { isValid } from "util/valid";
import {
  createCriteria,
  getCriteriaPlugin,
  searchCriteria,
  sectionName,
} from "./cohort";

type LocalSearchState = {
  search?: string;
};

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, section, sectionIndex } = useCohortGroupSectionAndGroup();

  const onInsertCriteria = useCallback(
    (criteria: tanagraUI.UICriteria) => {
      const group = insertCohortCriteria(context, section.id, criteria);
      navigate("../../" + cohortURL(cohort.id, section.id, group.id));
    },
    [context, cohort.id, section.id, navigate]
  );

  const name = sectionName(section, sectionIndex);

  return (
    <AddCriteria
      title={`Adding criteria for group ${name}`}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

export function AddFeatureSetCriteria() {
  const navigate = useNavigate();
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();

  const onInsertCriteria = useCallback(
    (criteria: tanagraUI.UICriteria) => {
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
      conceptSet
      title={`Adding criteria for ${featureSet.name}`}
      onInsertCriteria={onInsertCriteria}
      excludedPredefinedCriteria={featureSet.predefinedCriteria}
      onInsertPredefinedCriteria={onInsertPredefinedCriteria}
    />
  );
}

type AddCriteriaOption = {
  id: string;
  title: string;
  showMore: boolean;
  category?: string;
  tags?: string[];
  conceptSet?: boolean;
  criteriaConfig?: CriteriaConfig;
};

type AddCriteriaProps = {
  conceptSet?: boolean;
  title: string;
  backAction?: () => void;
  onInsertCriteria: (criteria: tanagraUI.UICriteria) => void;
  excludedPredefinedCriteria?: string[];
  onInsertPredefinedCriteria?: (criteria: string, title: string) => void;
};

function AddCriteria(props: AddCriteriaProps) {
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();

  const query = useLocalSearchState<LocalSearchState>()[0].search ?? "";
  const [globalSearchState, updateGlobalSearchState] = useGlobalSearchState();

  const criteriaConfigs = underlay.uiConfiguration.criteriaConfigs;
  const predefinedCriteria = underlay.uiConfiguration.prepackagedConceptSets;
  const searchConfig = underlay.uiConfiguration.criteriaSearchConfig;

  const options = useMemo(() => {
    const options: AddCriteriaOption[] = [];
    if (props.onInsertPredefinedCriteria) {
      predefinedCriteria.forEach((c) =>
        options.push({
          title: c.name,
          category: c.category ?? "Predefined",
          conceptSet: true,
          showMore: false,
          ...c,
        })
      );
    }

    criteriaConfigs.forEach((cc) =>
      options.push({
        criteriaConfig: cc,
        showMore: !!getCriteriaPlugin(createCriteria(source, cc)).renderEdit,
        ...cc,
      })
    );

    return options;
  }, [props.onInsertPredefinedCriteria, criteriaConfigs, predefinedCriteria]);

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
    () => new Map(options.map((o) => [o.id, o])),
    [options]
  );

  const selectedTags = useMemo(
    () => new Set(globalSearchState.addCriteriaTags ?? [tagList[0]]),
    [globalSearchState.addCriteriaTags, tagList]
  );

  const selectedOptions = useMemo(
    () =>
      options.filter((option) => {
        if (props.conceptSet && !option.conceptSet) {
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
      }),
    [selectedTags, options]
  );

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
    [underlay]
  );

  const onClick = useCallback(
    (option: AddCriteriaOption, dataEntry?: DataEntry) => {
      if (option.criteriaConfig) {
        const criteria = createCriteria(
          source,
          option.criteriaConfig,
          dataEntry
        );
        if (!!getCriteriaPlugin(criteria).renderEdit && !dataEntry) {
          navigate("../" + newCriteriaURL(option.id));
        } else {
          props.onInsertCriteria(criteria);
        }
      } else {
        props.onInsertPredefinedCriteria?.(option.id, option.title);
      }
    },
    [source, navigate]
  );

  const search = useCallback(async () => {
    const children: DataKey[] = [];
    const data: TreeGridData = {
      root: { data: {}, children },
    };

    if (query) {
      const res = await searchCriteria(
        source,
        selectedOptions.map((o) => o.criteriaConfig).filter(isValid),
        query
      );

      res.data.forEach((entry) => {
        const key = `${entry.source}~${entry.data.key}`;
        children.push(key);

        const item: CriteriaItem = {
          data: {
            ...entry.data,
            t_criteria_type: (
              <Stack direction="row" justifyContent="center">
                <Chip
                  label={optionsMap.get(entry.source)?.title}
                  size="small"
                />
              </Stack>
            ),
          },
          entry: entry,
        };
        data[key] = item;
      });
    }

    return data;
  }, [source, query, selectedOptions, optionsMap]);
  const searchState = useSWRImmutable<TreeGridData>(
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
              <FormControl>
                <Select
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
          <GridLayout cols spacing={0.5} rowAlign="middle">
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
          </GridLayout>
        </GridLayout>
        <GridLayout rows spacing={2} sx={{ height: "auto" }}>
          {categories.map((category) => (
            <GridLayout key={category[0].category} rows spacing={2}>
              <Typography variant="h6">{category[0].category}</Typography>
              <GridLayout cols spacing={2}>
                {category.flatMap((option, i) => {
                  const disabled =
                    (props.excludedPredefinedCriteria?.indexOf(option.id) ??
                      -1) >= 0;
                  const button = (
                    <Button
                      key={option.id}
                      data-testid={option.id}
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
                      <Divider key="t_divider" orientation="vertical" />,
                      button,
                    ];
                  }
                  return button;
                })}
              </GridLayout>
            </GridLayout>
          ))}
          {!!query ? <Divider /> : null}
        </GridLayout>
        {!!query ? (
          <Paper>
            <Loading status={searchState}>
              {!searchState.data?.root?.children?.length ? (
                <Empty
                  minHeight="300px"
                  image={emptyImage}
                  title="No matches found"
                />
              ) : (
                <TreeGrid
                  columns={columns}
                  data={searchState.data ?? {}}
                  rowCustomization={(
                    id: TreeGridId,
                    rowData: TreeGridRowData
                  ) => {
                    if (!searchState.data) {
                      return undefined;
                    }

                    const item = searchState.data[id] as CriteriaItem;
                    const option = optionsMap.get(item.entry.source);
                    if (!option) {
                      throw new Error(
                        `Item source "${item.entry.source}" doesn't match any criteria config ID.`
                      );
                    }

                    return [
                      {
                        column: columns.length - 1,
                        content: (
                          <GridLayout colAlign="center">
                            <Button
                              data-testid={rowData[searchConfig.columns[0].key]}
                              onClick={() => onClick(option, item.entry.data)}
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
  entry: MergedItem<DataEntry>;
};
