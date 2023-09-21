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
import { createConceptSet, useConceptSetContext } from "conceptSetContext";
import { MergedItem } from "data/source";
import { useSource } from "data/sourceContext";
import { DataEntry, DataKey } from "data/types";
import { useCohortGroupSectionAndGroup, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useMemo } from "react";
import { cohortURL, newCriteriaURL, useExitAction } from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagraUI from "tanagra-ui";
import { CriteriaConfig } from "underlaysSlice";
import {
  useGlobalSearchState,
  useLocalSearchState,
  useNavigate,
} from "util/searchState";
import {
  createCriteria,
  getCriteriaPlugin,
  searchCriteria,
  sectionName,
} from "./cohort";

type LocalSearchState = {
  search?: string;
};

export function AddConceptSetCriteria() {
  const context = useConceptSetContext();
  const exit = useExitAction();

  const onInsertCriteria = useCallback(
    (criteria: tanagraUI.UICriteria) => {
      createConceptSet(context, criteria);
      exit();
    },
    [context]
  );

  return (
    <AddCriteria
      conceptSet
      title="Adding data feature"
      backAction={exit}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, section, sectionIndex } = useCohortGroupSectionAndGroup();

  const onInsertCriteria = useCallback(
    (criteria: tanagraUI.UICriteria) => {
      const group = insertCohortCriteria(context, section.id, criteria);
      navigate("../../" + cohortURL(cohort.id, section.id, group.id));
    },
    [context, cohort.id, section.id]
  );

  const name = sectionName(section, sectionIndex);

  return (
    <AddCriteria
      title={`Adding criteria for group ${name}`}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

type AddCriteriaProps = {
  conceptSet?: boolean;
  title: string;
  backAction?: () => void;
  onInsertCriteria: (criteria: tanagraUI.UICriteria) => void;
};

function AddCriteria(props: AddCriteriaProps) {
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();

  const query = useLocalSearchState<LocalSearchState>()[0].search ?? "";
  const [globalSearchState, updateGlobalSearchState] = useGlobalSearchState();

  const criteriaConfigs = underlay.uiConfiguration.criteriaConfigs;
  const searchConfig = underlay.uiConfiguration.criteriaSearchConfig;

  const criteriaConfigMap = useMemo(
    () =>
      new Map(
        criteriaConfigs.map((cc) => [
          cc.id,
          {
            config: cc,
            showMore: !!getCriteriaPlugin(createCriteria(source, cc))
              .renderEdit,
          },
        ])
      ),
    [criteriaConfigs]
  );

  const tagList = useMemo(
    () =>
      criteriaConfigs.reduce((out: string[], cc) => {
        cc.tags?.forEach((t) => {
          if (out.indexOf(t) < 0) {
            out.push(t);
          }
        });
        return out;
      }, []),
    [criteriaConfigs]
  );

  const selectedTags = new Set(
    globalSearchState.addCriteriaTags ?? [tagList[0]]
  );

  const categories = useMemo(() => {
    const categories: CriteriaConfig[][] = [];
    const re = new RegExp(query, "i");

    for (const config of criteriaConfigs) {
      if (props.conceptSet && !config.conceptSet) {
        continue;
      }

      if (query && config.title.search(re) < 0) {
        continue;
      }

      if (
        selectedTags.size &&
        config.tags?.length &&
        !config.tags?.reduce((out, t) => out || selectedTags.has(t), false)
      ) {
        continue;
      }

      let category: CriteriaConfig[] | undefined;
      for (const c of categories) {
        if (c[0].category === config.category) {
          category = c;
          break;
        }
      }

      if (category) {
        category.push(config);
      } else {
        categories.push([config]);
      }
    }

    for (const c of categories) {
      c.sort(
        (a, b) =>
          (criteriaConfigMap.get(b.id)?.showMore ? 1 : 0) -
          (criteriaConfigMap.get(a.id)?.showMore ? 1 : 0)
      );
    }

    return categories;
  }, [query, criteriaConfigs, criteriaConfigMap, selectedTags]);

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
    (config: CriteriaConfig, dataEntry?: DataEntry) => {
      const criteria = createCriteria(source, config, dataEntry);
      if (!!getCriteriaPlugin(criteria).renderEdit && !dataEntry) {
        navigate("../" + newCriteriaURL(config.id));
      } else {
        props.onInsertCriteria(criteria);
      }
    },
    [source]
  );

  const search = useCallback(async () => {
    const children: DataKey[] = [];
    const data: TreeGridData = {
      root: { data: {}, children },
    };

    if (query) {
      const res = await searchCriteria(source, criteriaConfigs, query);

      res.data.forEach((entry) => {
        const key = `${entry.source}~${entry.data.key}`;
        children.push(key);

        const item: CriteriaItem = {
          data: {
            ...entry.data,
            t_criteria_type: (
              <Stack direction="row" justifyContent="center">
                <Chip
                  label={criteriaConfigMap.get(entry.source)?.config?.title}
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
  }, [source, query, criteriaConfigs]);
  const searchState = useSWRImmutable<TreeGridData>(
    { component: "AddCriteria", underlayName: underlay.name, query: query },
    search
  );

  return (
    <GridLayout rows>
      <ActionBar title={props.title} backAction={props.backAction} />
      <GridLayout rows spacing={2} sx={{ px: 5, py: 2 }}>
        <GridBox>
          <Search placeholder="Search by code or description" />
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
                {category.flatMap((config, i) => {
                  const showMore = criteriaConfigMap.get(config.id)?.showMore;
                  const button = (
                    <Button
                      key={config.id}
                      data-testid={config.id}
                      variant="outlined"
                      startIcon={showMore ? <SearchIcon /> : <AddIcon />}
                      onClick={() => onClick(config)}
                    >
                      {config.title}
                    </Button>
                  );

                  if (
                    i > 0 &&
                    criteriaConfigMap.get(category[i - 1].id)?.showMore !=
                      showMore
                  ) {
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
                  image="/empty.svg"
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
                    const config = criteriaConfigMap.get(
                      item.entry.source
                    )?.config;
                    if (!config) {
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
                              onClick={() => onClick(config, item.entry.data)}
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
