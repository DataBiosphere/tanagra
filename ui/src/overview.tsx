import BarChartIcon from "@mui/icons-material/BarChart";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { defaultFilter, insertGroup } from "cohortsSlice";
import Empty from "components/empty";
import Loading from "components/loading";
import SelectablePaper from "components/selectablePaper";
import { FilterCountValue, useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import {
  useAppDispatch,
  useCohort,
  useCohortAndGroup,
  useUnderlay,
} from "hooks";
import { useCallback, useState } from "react";
import { Link as RouterLink, Outlet, useNavigate } from "react-router-dom";
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
import { cohortURL } from "router";
import * as tanagra from "tanagra-api";
import { ChartConfigProperty } from "underlaysSlice";
import { isValid } from "util/valid";
import {
  generateCohortFilter,
  getCriteriaPlugin,
  groupName,
  uiGroupFilterFromFilter,
} from "./cohort";

export function Overview() {
  const [showDemographics, setShowDemographics] = useState(true);

  return (
    <Box
      sx={{
        display: "grid",
        width: "100%",
        height: "100%",
        gridTemplateColumns: (theme) =>
          `280px 1fr ${showDemographics ? "380px" : theme.spacing(8)}`,
        gridTemplateRows: "1fr",
        gridTemplateAreas: "'outline content drawer'",
      }}
    >
      <Box
        sx={{
          gridArea: "outline",
          p: 1,
        }}
      >
        <Outline />
      </Box>
      <Box
        sx={{
          gridArea: "content",
          height: "100%",
          minHeight: "100%",
          overflow: "auto",
          borderColor: (theme) => theme.palette.divider,
          borderStyle: "solid",
          borderWidth: "0px 1px",
        }}
      >
        <Box sx={{ minWidth: "900px", height: "100%" }}>
          <Outlet />
        </Box>
      </Box>
      <Box
        sx={{
          gridArea: "drawer",
          backgroundColor: (theme) => theme.palette.background.paper,
          p: 1,
          display: "grid",
        }}
      >
        <Box sx={{ gridArea: "1/1" }}>
          <DemographicCharts open={showDemographics} />
        </Box>
        <Box sx={{ gridArea: "1/1" }}>
          {showDemographics ? (
            <IconButton
              size="large"
              sx={{ float: "right" }}
              onClick={() => setShowDemographics(false)}
            >
              <ChevronRightIcon />
            </IconButton>
          ) : (
            <Button
              color="primary"
              sx={{ minWidth: "auto", width: "100%" }}
              onClick={() => setShowDemographics(true)}
            >
              <Stack alignItems="center">
                <BarChartIcon
                  sx={{ fontSize: "32px", transform: "rotate(90deg)" }}
                />
                <Typography variant="h4" sx={{ writingMode: "vertical-rl" }}>
                  Demographics
                </Typography>
              </Stack>
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}

function Outline() {
  const { cohort, group } = useCohortAndGroup();

  return (
    <Box className="outline">
      <Stack
        direction="row"
        alignItems="center"
        sx={{ height: (theme) => theme.spacing(6) }}
      >
        <Typography variant="h2" sx={{ mr: 1 }}>
          Requirements
        </Typography>
      </Stack>
      <List sx={{ p: 0 }}>
        {cohort.groups.map((g, index) => (
          <ListItemButton
            sx={{ p: 0, mb: 1 }}
            component={RouterLink}
            key={g.id}
            to={"../" + cohortURL(cohort.id, g.id)}
          >
            <ParticipantsGroup
              group={g}
              index={index}
              selected={group.id === g.id}
            />
          </ListItemButton>
        ))}
        <ListItem disableGutters key="" sx={{ p: 0 }}>
          <AddGroupButton />
        </ListItem>
      </List>
    </Box>
  );
}

function AddGroupButton() {
  const cohort = useCohort();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const onAddGroup = () => {
    const action = dispatch(insertGroup(cohort.id));
    const groupId = action.payload.group.id;
    if (groupId) {
      navigate(`../${cohortURL(cohort.id, groupId)}/add`);
    }
  };

  return (
    <>
      <Button onClick={onAddGroup} variant="contained" className="add-group">
        Add requirement
      </Button>
    </>
  );
}

function ParticipantsGroup(props: {
  group: tanagra.Group;
  index: number;
  selected: boolean;
}) {
  const source = useSource();
  const cohort = useCohort();

  const fetchGroupCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [
        {
          ...props.group,
          filter: {
            kind: props.group.filter.kind,
            excluded: false,
          },
        },
      ],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.id, cohort.name, cohort.underlayName, props.group]);

  const groupCountState = useAsyncWithApi(fetchGroupCount);

  return (
    <SelectablePaper selected={props.selected}>
      <Stack spacing={0} sx={{ p: 1 }}>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          className="group-title"
        >
          <Typography variant="h3">
            {groupName(props.group, props.index)}
          </Typography>
          <Chip label={uiGroupFilterFromFilter(props.group.filter)} />
        </Stack>
        {props.group.criteria.length === 0 ? (
          <Empty maxWidth="90%" minHeight="60px" title="No criteria yet" />
        ) : (
          props.group.criteria.map((criteria) => (
            <Box key={criteria.id}>
              <ParticipantCriteria group={props.group} criteria={criteria} />
            </Box>
          ))
        )}
        <Box
          key="footer"
          display="flex"
          flexDirection="row"
          justifyContent="right"
          alignItems="center"
          className="group-title"
        >
          <Loading status={groupCountState} size="small">
            <Typography variant="subtitle1">
              Requirement count: {groupCountState.data?.toLocaleString()}
            </Typography>
          </Loading>
        </Box>
      </Stack>
    </SelectablePaper>
  );
}

function ParticipantCriteria(props: {
  group: tanagra.Group;
  criteria: tanagra.Criteria;
}) {
  const source = useSource();
  const cohort = useCohort();

  const fetchCriteriaCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [
        {
          id: props.group.id,
          filter: defaultFilter,
          criteria: [props.criteria],
        },
      ],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.id, cohort.name, cohort.underlayName, props.criteria]);

  const criteriaCountState = useAsyncWithApi(fetchCriteriaCount);

  const details = getCriteriaPlugin(props.criteria).displayDetails();
  const title = details.standaloneTitle
    ? details.title
    : `${props.criteria.config.title}: ${details.title}`;

  const additionalText = details.additionalText?.length
    ? details.additionalText.join("\n")
    : title;

  return (
    <>
      <Box
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
        alignItems="center"
      >
        <Typography
          variant="body1"
          title={additionalText}
          sx={{
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
            overflow: "hidden",
          }}
        >
          {title}
        </Typography>
        <Loading status={criteriaCountState} size="small">
          <Typography variant="body2" sx={{ ml: 1 }}>
            {criteriaCountState.data?.toLocaleString()}
          </Typography>
        </Loading>
      </Box>
    </>
  );
}

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

type DemographicChartsProps = {
  open: boolean;
};

function DemographicCharts({ open }: DemographicChartsProps) {
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

  const demographicState = useAsyncWithApi(fetchDemographicData);

  const tickFormatter = (value: string) => {
    return value.length > 15 ? value.substr(0, 15).concat("…") : value;
  };

  if (!open) {
    // Keep the component in the hierarchy when hidden so the chart data stays
    // loaded. We may also display a mini version here instead.
    return null;
  }

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
