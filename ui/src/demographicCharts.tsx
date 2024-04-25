import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import Empty from "components/empty";
import Loading from "components/loading";
import { Cohort, FilterCountValue } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useStudyId, useUnderlay } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useCallback } from "react";
import useSWRImmutable from "swr/immutable";
import { ChartConfigProperty } from "underlaysSlice";
import { isValid } from "util/valid";
import { useVizPlugin, VizData } from "viz/viz";
import { generateCohortFilter } from "./cohort";

type BarData = {
  name: string;
  counts: Map<string, number>;
};

type ChartData = {
  title: string;
  stackedProperties: string[];
  bars: BarData[];
};

type StackedBarChartProps = {
  chart: ChartData;
};

const vizConfig = {};

function StackedBarChart({ chart }: StackedBarChartProps) {
  const viz = useVizPlugin("bar", vizConfig);
  const vizData = chart.bars
    .map((b) => {
      const data: VizData[] = [];
      b.counts.forEach((count, segment) =>
        data.push({
          keys: [
            { stringId: b.name, name: b.name },
            ...(b.counts.size > 1
              ? [{ stringId: segment, name: segment }]
              : []),
          ],
          values: [{ numeric: count }],
        })
      );
      return data;
    })
    .flat();

  return (
    <GridLayout rows>
      <Typography variant="body1">{chart.title}</Typography>
      {viz.render(vizData)}
    </GridLayout>
  );
}

export type DemographicChartsProps = {
  cohort: Cohort;
  separateCharts?: boolean;
  extraControls?: ReactNode;
};

export function DemographicCharts({
  cohort,
  extraControls,
}: DemographicChartsProps) {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const generatePropertyString = (
    property: ChartConfigProperty,
    filterCountValue: FilterCountValue
  ) => {
    let propertyString = "";
    const value = filterCountValue[property.key];

    if (isValid(value) && property.buckets) {
      property.buckets.forEach((range) => {
        const min = range.min;
        const max = range.max;
        const displayName = range.displayName;
        if (
          isValid(value) &&
          ((min && max && min <= value && value < max) ||
            (min && !max && min <= value) ||
            (!min && max && value < max))
        ) {
          propertyString = displayName;
        }
      });
    } else {
      propertyString = isValid(value) ? value.toString() : "Unknown";
    }
    return propertyString;
  };

  const fetchDemographicData = useCallback(async () => {
    const groupByAttributes =
      underlay.uiConfiguration.demographicChartConfigs.groupByAttributes;

    const demographicData = process.env.REACT_APP_BACKEND_FILTERS
      ? await studySource.cohortCount(
          studyId,
          cohort.id,
          undefined,
          undefined,
          groupByAttributes
        )
      : await underlaySource.filterCount(
          generateCohortFilter(underlaySource, cohort),
          groupByAttributes
        );

    const chartConfigs =
      underlay.uiConfiguration.demographicChartConfigs.chartConfigs;

    let totalCount = 0;
    const chartsData: ChartData[] = chartConfigs.map((config) => ({
      title: config.title,
      stackedProperties: [],
      bars: [],
    }));

    for (let i = 0; i < demographicData.length; i++) {
      const count = demographicData[i].count ?? 0;
      totalCount += count;
      chartConfigs.forEach((config, chartIndex) => {
        const currChart = chartsData[chartIndex];
        const primaryPropertyComponents: string[] = [];
        config.primaryProperties.forEach((property) => {
          primaryPropertyComponents.push(
            generatePropertyString(property, demographicData[i])
          );
        });

        const primaryPropertyString = primaryPropertyComponents.join(" ");
        let barIndex = currChart.bars.findIndex(
          (bar) => bar.name === primaryPropertyString
        );

        if (barIndex === -1) {
          currChart.bars.push({
            name: primaryPropertyString,
            counts: new Map(),
          });
          barIndex = currChart.bars.length - 1;
        }

        if (config.stackedProperty) {
          const stackPropertyString = generatePropertyString(
            config.stackedProperty,
            demographicData[i]
          );

          if (!currChart.stackedProperties.includes(stackPropertyString)) {
            currChart.stackedProperties.push(stackPropertyString);
          }

          if (!currChart.bars[barIndex].counts.has(stackPropertyString)) {
            currChart.bars[barIndex].counts.set(stackPropertyString, 0);
          }

          const prevCount =
            currChart.bars[barIndex].counts.get(stackPropertyString) ?? 0;
          currChart.bars[barIndex].counts.set(
            stackPropertyString,
            prevCount + count
          );
        } else {
          const prevCount = currChart.bars[barIndex].counts.get("count") ?? 0;
          currChart.bars[barIndex].counts.set("count", prevCount + count);
        }
      });
    }

    chartsData.forEach((chart) => {
      chart.bars.sort((a, b) => (a.name > b.name ? 1 : -1));
      chart.stackedProperties.sort();
    });

    return {
      totalCount,
      chartsData,
    };
  }, [underlay, cohort]);

  const demographicState = useSWRImmutable(
    { component: "DemographicCharts", underlayName: underlay.name, cohort },
    fetchDemographicData
  );

  return (
    <>
      <GridLayout rows spacing={3}>
        <GridLayout cols fillCol={2} rowAlign="bottom">
          <Typography variant="h6">Total count:&nbsp;</Typography>
          <Loading size="small" status={demographicState}>
            <Typography variant="h6">
              {demographicState.data?.totalCount.toLocaleString()}
            </Typography>
          </Loading>
          <GridBox />
          {extraControls}
        </GridLayout>
        <Paper
          sx={{
            p: 2,
            minHeight: "400px",
          }}
        >
          <Loading status={demographicState}>
            {demographicState.data?.totalCount ? (
              <GridLayout rows>
                {demographicState.data?.chartsData.map((chart, index) => {
                  return (
                    <StackedBarChart key={index} chart={chart as ChartData} />
                  );
                })}
              </GridLayout>
            ) : (
              <Empty
                minHeight="300px"
                image={emptyImage}
                title="No participants match the selected cohort"
                subtitle="You can broaden the cohort by removing criteria or selecting different requirement types"
              />
            )}
          </Loading>
        </Paper>
      </GridLayout>
    </>
  );
}
