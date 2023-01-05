import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import Empty from "components/empty";
import Loading from "components/loading";
import { FilterCountValue, useSource } from "data/source";
import { useCohort, useUnderlay } from "hooks";
import { useCallback } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  TooltipProps,
  XAxis,
  YAxis,
} from "recharts";
import useSWRImmutable from "swr/immutable";
import { ChartConfigProperty } from "underlaysSlice";
import { isValid } from "util/valid";
import { generateCohortFilter } from "./cohort";

const barColours = [
  "#0a96aa",
  "#e60049",
  "#0bb4ff",
  "#50e991",
  "#e6d800",
  "#9b19f5",
  "#ffa300",
  "#dc0ab4",
  "#b3d4ff",
  "#00bfa0",
];

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
  tickFormatter: (label: string) => string;
};

function StackedBarChart({ chart, tickFormatter }: StackedBarChartProps) {
  const barData = chart.bars.map((bar) => {
    return {
      name: bar.name,
      ...Object.fromEntries(bar.counts),
    };
  });
  return (
    <>
      <Typography variant="h4">{chart.title}</Typography>
      <ResponsiveContainer width="100%" height={30 + barData.length * 30}>
        <BarChart data={barData} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" />
          <YAxis
            dataKey="name"
            type="category"
            width={120}
            tickFormatter={tickFormatter}
            tickMargin={10}
          />
          <Tooltip
            content={(props: TooltipProps<number, string>) => {
              return (
                <Paper elevation={1} sx={{ p: 1 }}>
                  <Stack>
                    <Typography variant="h4">{props.label}</Typography>
                    {props.payload?.map((row) => (
                      <Stack key={row.name} direction="row" sx={{ mt: 1 }}>
                        <Box
                          sx={{
                            width: "20px",
                            height: "20px",
                            backgroundColor: row.color,
                            mr: 1,
                          }}
                        />
                        <Typography variant="body1">
                          {row.name}: {row.value}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                </Paper>
              );
            }}
          />
          {chart.stackedProperties.length > 0 ? (
            chart.stackedProperties.map((property, index) => (
              <Bar
                key={index}
                dataKey={property as string}
                stackId="a"
                fill={barColours[index % barColours.length]}
                maxBarSize={100}
              />
            ))
          ) : (
            <Bar dataKey="count" fill={barColours[0]} maxBarSize={60} />
          )}
        </BarChart>
      </ResponsiveContainer>
    </>
  );
}

export function DemographicCharts() {
  const cohort = useCohort();
  const underlay = useUnderlay();
  const source = useSource();

  const generatePropertyString = (
    property: ChartConfigProperty,
    filterCountValue: FilterCountValue
  ) => {
    let propertyString = "";
    // TODO(neelismail): Remove property key check once API supports age.
    let value =
      filterCountValue[property.key === "age" ? "year_of_birth" : property.key];

    // TODO(neelismail): Remove age handling once the API supports them.
    if (isValid(value) && property.key === "age" && typeof value === "number") {
      value = new Date().getFullYear() - value;
    }

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

    const demographicData = await source.filterCount(
      generateCohortFilter(cohort),
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

  const tickFormatter = (value: string) => {
    return value.length > 15 ? value.substr(0, 15).concat("…") : value;
  };

  return (
    <>
      <Loading status={demographicState}>
        <Grid item xs={1}>
          {demographicState.data?.totalCount ? (
            <Stack>
              <Typography variant="h2">{`Total count: ${demographicState.data?.totalCount.toLocaleString()}`}</Typography>
              {demographicState.data?.chartsData.map((chart, index) => {
                return (
                  <StackedBarChart
                    key={index}
                    chart={chart as ChartData}
                    tickFormatter={tickFormatter}
                  />
                );
              })}
            </Stack>
          ) : (
            <Empty
              minHeight="300px"
              image="/empty.png"
              title="No participants match the selected cohort"
              subtitle="You can broaden the cohort by removing criteria or selecting different requirement types"
            />
          )}
        </Grid>
      </Loading>
    </>
  );
}
