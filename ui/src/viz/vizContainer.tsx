import Typography from "@mui/material/Typography";
import { generateCohortFilter } from "cohort";
import Empty from "components/empty";
import Loading from "components/loading";
import { VALUE_SUFFIX } from "data/configuration";
import { Cohort, StudySource, UnderlaySource } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useCohort, useStudyId } from "hooks";
import emptyImage from "images/empty.svg";
import GridLayout from "layout/gridLayout";
import * as configProto from "proto/viz/viz_config";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";
import { getVizPlugin, VizData } from "viz/viz";

type VizContainerProps = {
  configs: configProto.VizConfig[];
  cohort: Cohort;
};

export function VizContainer(props: VizContainerProps) {
  const cohort = useCohort();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const dataState = useSWRImmutable(
    { type: "vizData", configs: props.configs, cohort: props.cohort },
    async () =>
      await Promise.all(
        props.configs.map((c) =>
          fetchVizData(c, underlaySource, studySource, studyId, cohort)
        )
      ).then((res) =>
        res.map((data, i) => {
          const config = props.configs[i];
          return {
            config,
            data,
            plugin: getVizPlugin(
              config.vizPlugin,
              JSON.parse(config.vizData ?? "{}")
            ),
          };
        })
      )
  );

  return (
    <GridLayout rows height="auto">
      <Loading status={dataState} immediate>
        <GridLayout rows>
          {dataState.data?.map((d, i) => (
            <GridLayout key={i} rows>
              <Typography variant="body1">
                {d.config.display?.title ?? "Untitled"}
              </Typography>
              {d.data?.length ? (
                d.plugin.render(d.data ?? [])
              ) : (
                <Empty
                  minHeight="150px"
                  image={emptyImage}
                  title="No data to display"
                />
              )}
            </GridLayout>
          ))}
        </GridLayout>
      </Loading>
    </GridLayout>
  );
}

async function fetchVizData(
  vizConfig: configProto.VizConfig,
  underlaySource: UnderlaySource,
  studySource: StudySource,
  studyId: string,
  cohort: Cohort
): Promise<VizData[]> {
  const vizSource = vizConfig.sources[0];

  // TODO(tjennison): Remove these limitations once the backend sufficiently
  // supports the query generation.
  if (!vizSource || vizConfig.sources.length > 1) {
    throw new Error("Only 1 visualization source is supported.");
  }
  if (vizSource.criteriaSelector !== "outputUnfiltered") {
    throw new Error("Only visualizations of the primary entity are supported.");
  }
  if (vizSource.joins?.length) {
    throw new Error("Joins are unsupported.");
  }
  if (!vizSource.attributes?.length || vizSource.attributes.length > 2) {
    throw new Error("Only 1 or 2 attributes are supported.");
  }

  const data = process.env.REACT_APP_BACKEND_FILTERS
    ? await studySource.cohortCount(
        studyId,
        cohort.id,
        undefined,
        undefined,
        vizSource.attributes.map((a) => a.attribute)
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

  return Array.from(dataMap.values());
}
