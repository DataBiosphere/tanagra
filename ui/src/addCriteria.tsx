import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import IconButton from "@mui/material/IconButton";
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
} from "components/treegrid";
import { createConceptSet, useConceptSetContext } from "conceptSetContext";
import { MergedDataEntry, useSource } from "data/source";
import { DataEntry, DataKey } from "data/types";
import { useCohortAndGroupSection, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
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
      title="New data feature"
      backURL={backURL}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, section } = useCohortAndGroupSection();

  const onInsertCriteria = useCallback(
    (criteria: tanagra.Criteria) => {
      insertCohortCriteria(context, section.id, criteria);
      navigate("../../" + cohortURL(cohort.id, section.id));
    },
    [context, cohort.id, section.id]
  );

  return (
    <AddCriteria title="Add criteria" onInsertCriteria={onInsertCriteria} />
  );
}

type AddCriteriaProps = {
  conceptSet?: boolean;
  title: string;
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
    <GridBox
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
        overflowY: "auto",
        px: 5,
        py: 3,
      }}
    >
      <ActionBar title={props.title} backURL={props.backURL} />
      <GridLayout rows spacing={3}>
        <Search placeholder="Search by code or description" />
        <Loading status={searchState}>
          {!searchState.data?.root?.children?.length ? (
            query !== "" ? (
              <Empty
                minHeight="300px"
                image="/empty.svg"
                title="No matches found"
              />
            ) : (
              <GridLayout rows height="auto">
                {categories.map((category) => (
                  <GridLayout key={category[0].category} rows height="auto">
                    <GridBox
                      key={category[0].category}
                      sx={{
                        p: 1.5,
                        backgroundColor: (theme) => theme.palette.info.main,
                        borderBottomStyle: "solid",
                        borderColor: (theme) => theme.palette.divider,
                        borderWidth: "2px",
                        height: "auto",
                      }}
                    >
                      <Typography key="" variant="overline">
                        {category[0].category ?? "Uncategorized"}
                      </Typography>
                    </GridBox>
                    {category.map((config) => (
                      <GridLayout
                        key={config.id}
                        cols
                        fillCol={1}
                        rowAlign="middle"
                        sx={{
                          p: 1.5,
                          borderBottomStyle: "solid",
                          borderColor: (theme) => theme.palette.divider,
                          borderWidth: "1px",
                          height: "auto",
                        }}
                      >
                        <Typography variant="body2">{config.title}</Typography>
                        <GridBox />
                        <GridLayout
                          colAlign="center"
                          width="100px"
                          height="auto"
                        >
                          {criteriaConfigMap.get(config.id)?.showMore ? (
                            <IconButton
                              key={config.id}
                              data-testid={config.id}
                              onClick={() => onClick(config)}
                            >
                              <NavigateNextIcon />
                            </IconButton>
                          ) : (
                            <Button
                              key={config.id}
                              data-testid={config.id}
                              onClick={() => onClick(config)}
                              variant="outlined"
                            >
                              Add
                            </Button>
                          )}
                        </GridLayout>
                      </GridLayout>
                    ))}
                  </GridLayout>
                ))}
              </GridLayout>
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
                const config = criteriaConfigMap.get(item.entry.source)?.config;
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
      </GridLayout>
    </GridBox>
  );
}

type CriteriaItem = TreeGridItem & {
  entry: MergedDataEntry;
};
