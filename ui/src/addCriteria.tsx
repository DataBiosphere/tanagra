import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { insertCohortCriteria, useCohortContext } from "cohortContext";
import CohortToolbar from "cohortToolbar";
import Empty from "components/empty";
import Loading from "components/loading";
import { Search } from "components/search";
import {
  TreeGrid,
  TreeGridData,
  TreeGridId,
  TreeGridItem,
} from "components/treegrid";
import { createConceptSet, useConceptSetContext } from "conceptSetContext";
import { MergedDataEntry, useSource } from "data/source";
import { DataEntry, DataKey } from "data/types";
import { useCohortAndGroup, useUnderlay } from "hooks";
import { useCallback, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { cohortURL, exitURL, newCriteriaURL, useBaseParams } from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";
import { CriteriaConfig } from "underlaysSlice";
import { createCriteria, getCriteriaPlugin, searchCriteria } from "./cohort";

export function AddConceptSetCriteria() {
  const navigate = useNavigate();
  const context = useConceptSetContext();

  const params = useBaseParams();
  const backURL = exitURL(params);

  const onInsertCriteria = useCallback(
    (criteria: tanagra.Criteria) => {
      createConceptSet(context, criteria);
      navigate(backURL);
    },
    [context]
  );

  return (
    <AddCriteria
      conceptSet
      backURL={backURL}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, group } = useCohortAndGroup();

  const onInsertCriteria = useCallback(
    (criteria: tanagra.Criteria) => {
      insertCohortCriteria(context, group.id, criteria);
      navigate("../../" + cohortURL(cohort.id, group.id));
    },
    [context, cohort.id, group.id]
  );

  return <AddCriteria onInsertCriteria={onInsertCriteria} />;
}

type AddCriteriaProps = {
  conceptSet?: boolean;
  backURL?: string;
  onInsertCriteria: (criteria: tanagra.Criteria) => void;
};

function AddCriteria(props: AddCriteriaProps) {
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();

  const query = useSearchParams()[0].get("search") ?? "";

  const criteriaConfigs = underlay.uiConfiguration.criteriaConfigs;
  const searchConfig = underlay.uiConfiguration.criteriaSearchConfig;

  const categories = useMemo(() => {
    const categories: CriteriaConfig[][] = [];

    for (const config of criteriaConfigs) {
      if (props.conceptSet && !config.conceptSet) {
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
      if (!!getCriteriaPlugin(criteria).renderEdit && !dataEntry) {
        navigate("../" + newCriteriaURL(config.id));
      } else {
        props.onInsertCriteria(criteria);
      }
    },
    [source]
  );

  const criteriaConfigMap = useMemo(
    () => new Map(criteriaConfigs.map((cc) => [cc.id, cc])),
    [criteriaConfigs]
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

    return data;
  }, [source, query, criteriaConfigs]);
  const searchState = useSWRImmutable<TreeGridData>(
    { component: "AddCriteria", underlayName: underlay.name, query: query },
    search
  );

  return (
    <Box
      sx={{
        p: 1,
        minHeight: "100%",
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <Search placeholder="Search criteria or select from the options below" />
      <ActionBar
        title={"Add criteria"}
        extraControls={!props.conceptSet ? <CohortToolbar /> : undefined}
        backURL={props.backURL}
      />
      <Loading status={searchState}>
        {!searchState.data?.root?.children?.length ? (
          query !== "" ? (
            <Empty
              minHeight="300px"
              image="/empty.png"
              title="No matches found"
            />
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
          )
        ) : (
          <TreeGrid
            columns={columns}
            data={searchState.data ?? {}}
            rowCustomization={(id: TreeGridId) => {
              if (!searchState.data) {
                return undefined;
              }

              const item = searchState.data[id] as CriteriaItem;
              const config = criteriaConfigMap.get(item.entry.source);
              if (!config) {
                throw new Error(
                  `Item source "${item.entry.source}" doesn't match any criteria config ID.`
                );
              }

              return [
                {
                  column: 1,
                  onClick: () => onClick(config, item.entry.data),
                },
              ];
            }}
          />
        )}
      </Loading>
    </Box>
  );
}

type CriteriaItem = TreeGridItem & {
  entry: MergedDataEntry;
};
