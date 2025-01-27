import BarChartIcon from "@mui/icons-material/BarChart";
import TableViewIcon from "@mui/icons-material/TableView";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Typography from "@mui/material/Typography";
import Empty from "components/empty";
import Loading from "components/loading";
import { Cohort, StudySource } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useCohort, useStudyId } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox, GridBoxPaper } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/viz/viz_data_config";
import { useMemo, useState } from "react";
import useSWRImmutable from "swr/immutable";
import { getVizPlugin, processFilterCountValues, VizData } from "viz/viz";

type UnparsedConfig = {
  dataConfig: string;
  plugin: string;
  pluginConfig: string;
  title: string;
};

type ParsedConfig = {
  dataConfig: configProto.VizDataConfig;
  plugin: string;
  pluginConfig: object;
  title: string;
};

type VizContainerProps = {
  config: UnparsedConfig;
  cohort: Cohort;
};

export function VizContainer(props: VizContainerProps) {
  const cohort = useCohort();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const [view, setView] = useState("plugin");

  const config = useMemo(
    (): ParsedConfig => ({
      ...props.config,
      dataConfig: configProto.VizDataConfig.fromJSON(
        JSON.parse(props.config.dataConfig)
      ),
      pluginConfig: JSON.parse(props.config.pluginConfig ?? "{}"),
    }),
    [props.config]
  );

  const dataState = useSWRImmutable(
    { type: "vizData", config: config, cohort: props.cohort },
    async () =>
      fetchVizData(config.dataConfig, studySource, studyId, cohort).then(
        (data) => ({
          data,
          plugin: getVizPlugin(config.plugin, config.pluginConfig),
          tablePlugin: getVizPlugin("core/table", {
            keyTitles: config.dataConfig.sources
              .map((s) => s.attributes.map((a) => a.attribute))
              .flat(),
          }),
        })
      )
  );

  const handleViewChange = (
    event: React.MouseEvent<HTMLElement>,
    view: string | null
  ) => {
    setView(view ?? "plugin");
  };

  return (
    <GridBoxPaper
      sx={{
        p: 2,
        borderWidth: "1px",
        borderColor: (theme) => theme.palette.divider,
        borderStyle: "solid",
      }}
    >
      <Loading status={dataState} immediate>
        <GridLayout rows>
          <GridLayout cols={3} fillCol={1} rowAlign="middle">
            <Typography variant="body1em">{config.title}</Typography>
            <GridBox />
            <ToggleButtonGroup
              value={view}
              exclusive
              size="small"
              onChange={handleViewChange}
            >
              <ToggleButton value="plugin">
                <BarChartIcon />
              </ToggleButton>
              <ToggleButton value="table">
                <TableViewIcon />
              </ToggleButton>
            </ToggleButtonGroup>
          </GridLayout>
          {dataState.data?.data?.length ? (
            view === "table" ? (
              dataState.data.tablePlugin.render(dataState.data.data ?? [])
            ) : (
              dataState.data.plugin.render(dataState.data.data ?? [])
            )
          ) : (
            <Empty
              minHeight="150px"
              image={emptyImage}
              title="No data to display"
            />
          )}
        </GridLayout>
      </Loading>
    </GridBoxPaper>
  );
}

// TODO(tjennison): Since query generation has moved to the backend, the UI can
// no longer determine these relationships. Hardcode them here until viz query
// generation also moves to the backend.
const selectorToEntity: { [key: string]: string } = {
  demographics: "person",
  conditions: "conditionOccurrence",
  procedures: "procedureOccurrence",
  ingredients: "ingredientOccurrence",
  measurements: "measurementOccurrence",
};
async function fetchVizData(
  vizDataConfig: configProto.VizDataConfig,
  studySource: StudySource,
  studyId: string,
  cohort: Cohort
): Promise<VizData[]> {
  const vizSource = vizDataConfig.sources[0];

  // TODO(tjennison): Remove these limitations once the backend sufficiently
  // supports the query generation.
  if (!vizSource || vizDataConfig.sources.length > 1) {
    throw new Error("Only 1 visualization source is supported.");
  }
  const entity = selectorToEntity[vizSource.criteriaSelector];
  if (!entity) {
    throw new Error(
      `Visualizations of ${vizSource.criteriaSelector} are not supported.`
    );
  }
  if (vizSource.joins?.find((j) => j.entity !== "person" || j.aggregation)) {
    throw new Error("Only unique joins to person are supported.");
  }
  if (!vizSource.attributes?.length || vizSource.attributes.length > 2) {
    throw new Error("Only 1 or 2 attributes are supported.");
  }

  const fcvs = await studySource.cohortCount(studyId, cohort.id, {
    groupByAttributes: vizSource.attributes.map((a) => a.attribute),
    entity,
    countDistinctAttribute: vizSource.joins?.length ? "person_id" : undefined,
  });

  return processFilterCountValues(vizDataConfig, fcvs);
}
