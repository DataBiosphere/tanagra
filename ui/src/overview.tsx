import BarChartIcon from "@mui/icons-material/BarChart";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Drawer from "@mui/material/Drawer";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import { CSSObject, styled, Theme } from "@mui/material/styles";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { defaultFilter, insertGroup } from "cohortsSlice";
import Loading from "components/loading";
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
  groupFilterKindLabel,
  groupName,
} from "./cohort";

const demographicsWidth = 400;
const outlineWidth = 300;

// This drawer customization is taken directly from the MUI docs.
const openedMixin = (theme: Theme): CSSObject => ({
  width: demographicsWidth,
  transition: theme.transitions.create("width", {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.enteringScreen,
  }),
  overflowX: "hidden",
});

const closedMixin = (theme: Theme): CSSObject => ({
  transition: theme.transitions.create("width", {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  overflowX: "hidden",
  width: `calc(${theme.spacing(7)} + 1px)`,
  [theme.breakpoints.up("sm")]: {
    width: `calc(${theme.spacing(8)} + 1px)`,
  },
});

const MiniDrawer = styled(Drawer, {
  shouldForwardProp: (prop) => prop !== "open",
})(({ theme, open }) => ({
  width: demographicsWidth,
  flexShrink: 0,
  whiteSpace: "nowrap",
  boxSizing: "border-box",
  ...(open && {
    ...openedMixin(theme),
    "& .MuiDrawer-paper": openedMixin(theme),
  }),
  ...(!open && {
    ...closedMixin(theme),
    "& .MuiDrawer-paper": closedMixin(theme),
  }),
}));

export function Overview() {
  const [showDemographics, setShowDemographics] = useState(false);

  // TODO(tjennison): This overall layout is a mess and the built in components
  // are only making it more difficult. Overhaul the main layout to use basic
  // boxes and layout components, and confine scrolling to each of the
  // approprite panels.
  return (
    <Box sx={{ display: "flex", height: "calc(100% - 48px)" }}>
      <Drawer
        variant="permanent"
        anchor="left"
        sx={{
          width: outlineWidth,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: {
            backgroundColor: (theme) => theme.palette.background.default,
            width: outlineWidth,
            boxSizing: "border-box",
          },
        }}
      >
        <Toolbar />
        <Box sx={{ mx: 1 }}>
          <Outline />
        </Box>
      </Drawer>
      <Box
        sx={{
          flexGrow: 1,
          height: "100%",
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
      <MiniDrawer variant="permanent" open={showDemographics} anchor="right">
        <Toolbar />
        <Box sx={{ m: 1, display: "grid" }}>
          <Box sx={{ gridArea: "1/1" }}>
            <DemographicCharts open={showDemographics} />
          </Box>
          <Box sx={{ gridArea: "1/1" }}>
            <IconButton
              size="large"
              sx={{ float: "right" }}
              onClick={() => setShowDemographics(!showDemographics)}
            >
              {showDemographics ? <ChevronRightIcon /> : <BarChartIcon />}
            </IconButton>
          </Box>
        </Box>
      </MiniDrawer>
    </Box>
  );
}

function Outline() {
  const { cohort, group } = useCohortAndGroup();

  return (
    <Box className="outline">
      <List>
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
      navigate("../" + cohortURL(cohort.id, groupId));
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
    <Paper
      sx={{ p: 1, width: "100%", overflow: "hidden", position: "relative" }}
    >
      {props.selected && (
        <Box
          className="selected-group-highlight"
          sx={{
            position: "absolute",
            top: "16px",
            bottom: "16px",
            left: "-4px",
            width: "8px",
            "border-radius": "4px",
            backgroundColor: (theme) => theme.palette.primary.main,
          }}
        />
      )}
      <Stack spacing={0}>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          className="group-title"
        >
          <Typography variant="h3">
            {groupName(props.group, props.index)}
          </Typography>
          <Stack direction="row" spacing={1}>
            {props.group.filter.excluded && <Chip label="Not" />}
            <Chip label={groupFilterKindLabel(props.group.filter.kind)} />
          </Stack>
        </Stack>
        {props.group.criteria.map((criteria) => (
          <Box key={criteria.id}>
            <ParticipantCriteria group={props.group} criteria={criteria} />
          </Box>
        ))}
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
              Group Count: {groupCountState.data?.toLocaleString()}
            </Typography>
          </Loading>
        </Box>
      </Stack>
    </Paper>
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
      <ResponsiveContainer width="100%" height={barData.length * 50}>
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
          <Tooltip />
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
    const additionalSelectedAttributes = new Set<string>();
    underlay.uiConfiguration.demographicChartConfigs.chartConfigs.forEach(
      (config) => {
        config.primaryProperties.forEach((property) => {
          if (!groupByAttributes.includes(property.key)) {
            additionalSelectedAttributes.add(property.key);
          }
        });

        if (
          config.stackedProperty &&
          !groupByAttributes.includes(config.stackedProperty.key)
        ) {
          additionalSelectedAttributes.add(config.stackedProperty.key);
        }
      }
    );

    // TODO(neelismail): Remove guard for age property key when API provides age support
    if (additionalSelectedAttributes.has("age")) {
      additionalSelectedAttributes.delete("age");
      if (!groupByAttributes.includes("year_of_birth")) {
        additionalSelectedAttributes.add("year_of_birth");
      }
    }

    const demographicData = await source.filterCount(
      generateCohortFilter(cohort),
      groupByAttributes,
      Array.from(additionalSelectedAttributes)
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

    chartsData.forEach((chart) =>
      chart.bars.sort((a, b) => (a.name > b.name ? 1 : -1))
    );

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
          <Stack>
            <Typography variant="h2">{`Total Count: ${demographicState.data?.totalCount.toLocaleString()}`}</Typography>
            {demographicState.data?.totalCount &&
              demographicState.data?.chartsData.map((chart, index) => {
                return (
                  <StackedBarChart
                    key={index}
                    chart={chart as ChartData}
                    tickFormatter={tickFormatter}
                  />
                );
              })}
          </Stack>
        </Grid>
      </Loading>
    </>
  );
}
