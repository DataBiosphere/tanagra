import BarChartIcon from "@mui/icons-material/BarChart";
import TableViewIcon from "@mui/icons-material/TableView";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Typography from "@mui/material/Typography";
import { generateCohortFilter } from "cohort";
import Empty from "components/empty";
import Loading from "components/loading";
import { VALUE_SUFFIX } from "data/configuration";
import { Cohort, StudySource, UnderlaySource } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { compareDataValues } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { getEnvironment } from "environment";
import { useCohort, useStudyId } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox, GridBoxPaper } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/viz/viz_config";
import { useMemo, useState } from "react";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";
import { getVizPlugin, VizData } from "viz/viz";

type UnparsedConfig = {
  dataConfig: string;
  plugin: string;
  pluginConfig: string;
  title: string;
};

type ParsedConfig = {
  dataConfig: configProto.VizConfig;
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
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const [view, setView] = useState("plugin");

  const config = useMemo(
    (): ParsedConfig => ({
      ...props.config,
      dataConfig: configProto.VizConfig.fromJSON(
        JSON.parse(props.config.dataConfig)
      ),
      pluginConfig: JSON.parse(props.config.pluginConfig ?? "{}"),
    }),
    [props.config]
  );

  const dataState = useSWRImmutable(
    { type: "vizData", config: config, cohort: props.cohort },
    async () =>
      fetchVizData(
        config.dataConfig,
        underlaySource,
        studySource,
        studyId,
        cohort
      ).then((data) => ({
        data,
        plugin: getVizPlugin(config.plugin, config.pluginConfig),
        tablePlugin: getVizPlugin("core/table", {
          keyTitles: config.dataConfig.sources
            .map((s) => s.attributes.map((a) => a.attribute))
            .flat(),
        }),
      }))
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
  vizConfig: configProto.VizConfig,
  underlaySource: UnderlaySource,
  studySource: StudySource,
  studyId: string,
  cohort: Cohort
): Promise<VizData[]> {
  const env = getEnvironment();
  const vizSource = vizConfig.sources[0];

  // TODO(tjennison): Remove these limitations once the backend sufficiently
  // supports the query generation.
  if (!vizSource || vizConfig.sources.length > 1) {
    throw new Error("Only 1 visualization source is supported.");
  }
  const entity = selectorToEntity[vizSource.criteriaSelector];
  if (!entity || (!env.REACT_APP_BACKEND_FILTERS && entity !== "person")) {
    throw new Error(
      `Visualizations of ${vizSource.criteriaSelector} are not supported.`
    );
  }
  if (vizSource.joins?.length) {
    throw new Error("Joins are unsupported.");
  }
  if (!vizSource.attributes?.length || vizSource.attributes.length > 2) {
    throw new Error("Only 1 or 2 attributes are supported.");
  }

  const data = env.REACT_APP_BACKEND_FILTERS
    ? await studySource.cohortCount(
        studyId,
        cohort.id,
        undefined,
        undefined,
        vizSource.attributes.map((a) => a.attribute),
        entity
      )
    : await underlaySource.filterCount(
        generateCohortFilter(underlaySource, cohort),
        vizSource.attributes.map((a) => a.attribute)
      );

  const dataMap = new Map<string, VizData>();
  data.forEach((d) => {
    let exclude = false;
    const vd: VizData = {
      keys: vizSource.attributes.map((a) => {
        let value = d[a.attribute];
        if (!isValid(value)) {
          value = "Unknown";
        }

        let name: string | undefined = undefined;
        let numericId: number | undefined = undefined;
        let stringId: string | undefined = undefined;
        if (a.numericBucketing) {
          const thresholds = a.numericBucketing.thresholds ?? [];
          if (!thresholds.length) {
            const intervals = a.numericBucketing.intervals;
            if (!intervals || !intervals.min || !intervals.max) {
              throw new Error(
                "Bucketing is configured without thresholds or intervals."
              );
            }

            for (let i = 0; i < intervals.count + 1; i++) {
              thresholds.push(
                intervals.min + i * (intervals.max - intervals.min)
              );
            }
          }

          if (a.numericBucketing.includeLesser && value < thresholds[0]) {
            name = `<${thresholds[0]}`;
          } else {
            for (let i = 0; i < thresholds.length; i++) {
              if (value >= thresholds[i - 1] && value < thresholds[i]) {
                name = `${thresholds[i - 1]}-${thresholds[i]}`;
                break;
              }
            }
          }
          if (a.numericBucketing.includeGreater && !name) {
            name = `>=${thresholds[thresholds.length - 1]}`;
          }

          if (!name) {
            exclude = true;
            return { name: "" };
          }

          stringId = name;
        } else {
          name = String(value);
          const id = d[a.attribute + VALUE_SUFFIX];
          if (typeof id === "number") {
            numericId = id;
          } else {
            stringId = String(id) ?? name;
          }
        }

        return {
          name,
          numericId,
          stringId,
        };
      }),
      values: [{ numeric: d.count ?? 0 }],
    };

    if (exclude) {
      return;
    }

    // TODO(tjennison): Handle other values types.
    const keyId = vd.keys
      .map((k) => String(k.numericId ?? k.stringId))
      .join("~");
    const existing = dataMap.get(keyId);
    if (existing) {
      if (existing.values?.[0]?.numeric && vd.values?.[0]?.numeric) {
        existing.values[0].numeric += vd.values[0].numeric;
      }
    } else {
      dataMap.set(keyId, vd);
    }
  });

  const arr = Array.from(dataMap.values());
  arr.sort((a, b) => {
    for (let i = 0; i < vizSource.attributes.length; i++) {
      const attrib = vizSource.attributes[i];
      let sortValue = 0;
      if (
        !attrib.sortType ||
        attrib.sortType === configProto.VizConfig_Source_Attribute_SortType.NAME
      ) {
        sortValue = compareDataValues(a.keys[i].name, b.keys[i].name);
      } else if (
        attrib.sortType ===
        configProto.VizConfig_Source_Attribute_SortType.VALUE
      ) {
        sortValue = compareDataValues(a.values[i].numeric, b.values[i].numeric);
      }

      if (sortValue === 0) {
        sortValue = compareDataValues(a.keys[i].numericId, b.keys[i].numericId);
      }
      if (sortValue === 0) {
        sortValue = compareDataValues(a.keys[i].stringId, b.keys[i].stringId);
      }

      if (attrib.sortDescending) {
        sortValue = -sortValue;
      }

      if (sortValue !== 0) {
        return sortValue;
      }
    }
    return 0;
  });

  for (let i = 1; i < vizSource.attributes.length; i++) {
    if (vizSource.attributes[i].limit) {
      throw new Error(
        "A limit is only supported on the first visualization attribute."
      );
    }
  }

  const limit = vizSource.attributes[0].limit;
  if (limit) {
    let count = 0;
    for (let i = 1; i < arr.length; i++) {
      const ka = arr[i].keys[0];
      const kb = arr[i - 1].keys[0];
      if (ka.numericId !== kb.numericId || ka.stringId !== kb.stringId) {
        count++;
        if (count === limit) {
          arr.splice(i);
          break;
        }
      }
    }
  }

  return arr;
}
