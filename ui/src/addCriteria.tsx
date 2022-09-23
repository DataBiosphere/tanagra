import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { insertCriteria } from "cohortsSlice";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
} from "components/treegrid";
import { DataEntry, DataKey } from "data/configuration";
import { MergedDataEntry, useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useCohortAndGroup, useUnderlay } from "hooks";
import { useCallback, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL } from "router";
import { CriteriaConfig } from "underlaysSlice";
import { createCriteria, getCriteriaPlugin, searchCriteria } from "./cohort";

export function AddCriteria() {
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { cohort, group } = useCohortAndGroup();

  const [query, setQuery] = useState<string>("");
  const [showResults, setShowResults] = useState<boolean>(false);
  const [data, setData] = useState<TreeGridData>({});

  const criteriaConfigs = underlay.uiConfiguration.criteriaConfigs;
  const searchConfig = underlay.uiConfiguration.criteriaSearchConfig;

  const categories = useMemo(() => {
    const categories: CriteriaConfig[][] = [];

    for (const config of criteriaConfigs) {
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

    return categories;
  }, [underlay]);

  const columns = useMemo(
    () => [
      {
        key: "t_criteria_type",
        width: searchConfig.criteriaTypeWidth,
        title: "Criteria type",
      },
      ...searchConfig.columns,
    ],
    [underlay]
  );

  const onClick = useCallback(
    (config: CriteriaConfig, dataEntry?: DataEntry) => {
      const criteria = createCriteria(source, config, dataEntry);
      dispatch(
        insertCriteria({
          cohortId: cohort.id,
          groupId: group.id,
          criteria,
        })
      );
      navigate(
        !!getCriteriaPlugin(criteria).renderEdit && !dataEntry
          ? "../" + criteriaURL(criteria.id)
          : "../../" + cohortURL(cohort.id, group.id)
      );
    },
    [source, cohort.id, group.id]
  );

  const criteriaConfigMap = useMemo(
    () => new Map(criteriaConfigs.map((cc) => [cc.id, cc])),
    [criteriaConfigs]
  );

  const search = useCallback(async () => {
    // This prevents briefly showing the empty results from the empty query
    // inbetween when a new query has been set but before searchState is
    // retriggered.
    setShowResults(!!query);

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
                  label={criteriaConfigMap.get(entry.source)?.title}
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

    setData(data);
  }, [source, query, criteriaConfigs]);
  const searchState = useAsyncWithApi<void>(search);

  return (
    <Box
      sx={{
        p: 1,
        minWidth: "900px",
        height: "100%",
        overflow: "auto",
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <Search
        placeholder="Search criteria or select from the options below"
        onSearch={setQuery}
      />
      <ActionBar title={"Add criteria"} />
      {showResults ? (
        <Loading status={searchState}>
          <TreeGrid
            columns={columns}
            data={data}
            rowCustomization={(id: TreeGridId) => {
              const item = data[id] as CriteriaItem;
              const config = criteriaConfigMap.get(item.entry.source);
              if (!config) {
                throw new Error(
                  `Item source "${item.entry.source}" doesn't match any criteria config ID.`
                );
              }

              return new Map([
                [
                  1,
                  {
                    onClick: () => onClick(config, item.entry.data),
                  },
                ],
              ]);
            }}
          />
        </Loading>
      ) : (
        <Stack direction="row" justifyContent="center">
          {categories.map((category) => (
            <Stack
              key={category[0].category}
              spacing={1}
              alignItems="center"
              justifyContent="flex-start"
              sx={{ width: 180 }}
            >
              <Typography key="" variant="subtitle1">
                {category[0].category ?? "Uncategorized"}
              </Typography>
              {category.map((config) => (
                <Button
                  key={config.id}
                  onClick={() => onClick(config)}
                  variant="contained"
                >
                  {config.title}
                </Button>
              ))}
            </Stack>
          ))}
        </Stack>
      )}
    </Box>
  );
}

type CriteriaItem = TreeGridItem & {
  entry: MergedDataEntry;
};
