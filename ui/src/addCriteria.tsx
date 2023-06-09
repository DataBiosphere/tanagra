import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import { useTheme } from "@mui/material/styles";
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
import {
  createCriteria,
  getCriteriaPlugin,
  searchCriteria,
  sectionName,
} from "./cohort";

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
      title="Adding data feature"
      backURL={backURL}
      onInsertCriteria={onInsertCriteria}
    />
  );
}

export function AddCohortCriteria() {
  const navigate = useNavigate();
  const context = useCohortContext();
  const { cohort, section, sectionIndex } = useCohortAndGroupSection();

  const onInsertCriteria = useCallback(
    (criteria: tanagra.Criteria) => {
      insertCohortCriteria(context, section.id, criteria);
      navigate("../../" + cohortURL(cohort.id, section.id));
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
  backURL?: string;
  onInsertCriteria: (criteria: tanagra.Criteria) => void;
};

function AddCriteria(props: AddCriteriaProps) {
  const theme = useTheme();
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
    <GridLayout
      rows
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <ActionBar title={props.title} backURL={props.backURL} />
      <GridLayout rows>
        <GridBox sx={{ px: 5, py: 3 }}>
          <Search placeholder="Search by code or description" />
        </GridBox>
        <Loading status={searchState}>
          {!searchState.data?.root?.children?.length ? (
            query !== "" ? (
              <Empty
                minHeight="300px"
                image="/empty.svg"
                title="No matches found"
              />
            ) : (
              <GridBox sx={{ px: 5, overflowY: "auto" }}>
                <table
                  style={{
                    tableLayout: "fixed",
                    width: "100%",
                    textAlign: "left",
                    borderCollapse: "collapse",
                  }}
                >
                  <colgroup>
                    <col
                      style={{
                        width: "100%",
                        minWidth: "100%",
                        maxWidth: "100%",
                        verticalAlign: "center",
                      }}
                    />
                    <col
                      style={{
                        width: "80px",
                        minWidth: "80px",
                        maxWidth: "80px",
                        verticalAlign: "center",
                      }}
                    />
                  </colgroup>
                  <tbody>
                    {categories.map((category) => [
                      <tr key={category[0].category}>
                        <th
                          colSpan={2}
                          style={{
                            position: "sticky",
                            top: 0,
                            backgroundColor: theme.palette.info.main,
                            boxShadow: `inset 0 -2px 0 ${theme.palette.divider}`,
                            zIndex: 1,
                            height: theme.spacing(6),
                          }}
                        >
                          <Typography key="" variant="overline">
                            {category[0].category}
                          </Typography>
                        </th>
                      </tr>,
                      ...category.map((config) => (
                        <tr key={config.id}>
                          <td
                            style={{
                              boxShadow: `inset 0 -1px 0 ${theme.palette.divider}`,
                              height: theme.spacing(6),
                            }}
                          >
                            <Typography key="" variant="body2">
                              {config.title}
                            </Typography>
                          </td>
                          <td
                            style={{
                              boxShadow: `inset 0 -1px 0 ${theme.palette.divider}`,
                            }}
                          >
                            <GridLayout colAlign="center">
                              {criteriaConfigMap.get(config.id)?.showMore ? (
                                <Button
                                  key={config.id}
                                  data-testid={config.id}
                                  onClick={() => onClick(config)}
                                  variant="outlined"
                                >
                                  Explore
                                </Button>
                              ) : (
                                <Button
                                  key={config.id}
                                  data-testid={config.id}
                                  onClick={() => onClick(config)}
                                  variant="outlined"
                                  sx={{ minWidth: "auto" }}
                                >
                                  Add
                                </Button>
                              )}
                            </GridLayout>
                          </td>
                        </tr>
                      )),
                    ])}
                  </tbody>
                </table>
              </GridBox>
            )
          ) : (
            <TreeGrid
              columns={columns}
              data={searchState.data ?? {}}
              rowCustomization={(id: TreeGridId, rowData: TreeGridRowData) => {
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
      </GridLayout>
    </GridLayout>
  );
}

type CriteriaItem = TreeGridItem & {
  entry: MergedDataEntry;
};
