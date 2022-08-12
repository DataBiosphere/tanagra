import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { EntityCountsApiContext } from "apiContext";
import {
  deleteCriteria,
  deleteGroup,
  insertCriteria,
  insertGroup,
  renameCriteria,
  renameGroup,
} from "cohortsSlice";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useCohort, useUnderlay } from "hooks";
import { useCallback, useContext } from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { createUrl } from "router";
import * as tanagra from "tanagra-api";
import { ChartConfigProperty } from "underlaysSlice";
import {
  createCriteria,
  generateQueryFilter,
  getCriteriaPlugin,
} from "./cohort";

export default function Overview() {
  const cohort = useCohort();

  return (
    <>
      <ActionBar title={cohort.name} />
      <Grid container columns={3} columnSpacing={5} className="overview">
        <ParticipantsSelector kind={tanagra.GroupKindEnum.Included} />
        <ParticipantsSelector kind={tanagra.GroupKindEnum.Excluded} />
        <DemographicCharts cohort={cohort} />
      </Grid>
    </>
  );
}

function ParticipantsSelector(props: { kind?: tanagra.GroupKindEnum }) {
  const cohort = useCohort();

  return (
    <Grid item xs={1}>
      <Typography variant="h4">
        {props.kind === tanagra.GroupKindEnum.Included
          ? "Included Participants"
          : "Excluded Participants"}
      </Typography>
      <Stack spacing={0}>
        {cohort.groups
          .filter((g) => g.kind === props.kind)
          .map((group, index) => (
            <Box key={group.id}>
              <ParticipantsGroup group={group} index={index} />
              <Divider className="and-divider">
                <Chip label="AND" />
              </Divider>
            </Box>
          ))}
        <Box key="">
          <AddCriteriaButton kind={props.kind} />
        </Box>
      </Stack>
    </Grid>
  );
}

function AddCriteriaButton(props: {
  group?: string;
  kind?: tanagra.GroupKindEnum;
}) {
  const underlay = useUnderlay();
  const source = useSource();
  const cohort = useCohort();
  const history = useHistory();
  const dispatch = useAppDispatch();

  const configs = underlay.uiConfiguration.criteriaConfigs;

  const onAddCriteria = (criteria: tanagra.Criteria) => {
    let groupId = "";
    if (props.group) {
      groupId = props.group;
      dispatch(insertCriteria({ cohortId: cohort.id, groupId, criteria }));
    } else if (props.kind) {
      const action = dispatch(insertGroup(cohort.id, props.kind, criteria));
      groupId = action.payload.group.id;
    }
    if (groupId) {
      history.push(
        createUrl({
          underlayName: underlay.name,
          cohortId: cohort.id,
          groupId,
          criteriaId: criteria.id,
        })
      );
    }
  };

  const [menu, show] = useMenu({
    children: configs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onAddCriteria(createCriteria(source, config));
        }}
      >
        {config.title}
      </MenuItem>
    )),
  });

  return (
    <>
      <Button onClick={show} variant="contained" className="add-criteria">
        Add Criteria
      </Button>
      {menu}
    </>
  );
}

function ParticipantsGroup(props: { group: tanagra.Group; index: number }) {
  const dispatch = useAppDispatch();
  const cohort = useCohort();
  const groupName = props.group.name || "Group " + String(props.index + 1);
  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Edit Group Name",
    initialText: groupName,
    textLabel: "Group Name",
    buttonLabel: "Rename Group",
    onConfirm: (name: string) => {
      dispatch(
        renameGroup({
          cohortId: cohort.id,
          groupId: props.group.id,
          groupName: name,
        })
      );
    },
  });

  const [groupMenu, groupShow] = useMenu({
    children: [
      <MenuItem key="1" onClick={showRenameGroup}>
        Edit Group Name
      </MenuItem>,
      <MenuItem
        key="2"
        onClick={() =>
          dispatch(
            deleteGroup({
              cohortId: cohort.id,
              groupId: props.group.id,
            })
          )
        }
      >
        Delete Group
      </MenuItem>,
    ],
  });

  return (
    <Paper className="participants-group">
      <Grid container className="group-title">
        <Grid item xs="auto">
          <IconButton onClick={groupShow} component="span" size="small">
            <MoreVertIcon fontSize="small" />
          </IconButton>
          {groupMenu}
          {renameGroupDialog}
        </Grid>
        <Grid item>
          <Typography variant="h5">{groupName}</Typography>
        </Grid>
      </Grid>
      <Stack spacing={0}>
        {props.group.criteria.map((criteria) => (
          <Box key={criteria.id}>
            <ParticipantCriteria group={props.group} criteria={criteria} />
            <Divider>OR</Divider>
          </Box>
        ))}
        <Box key="">
          <AddCriteriaButton group={props.group.id} />
        </Box>
      </Stack>
    </Paper>
  );
}

function ParticipantCriteria(props: {
  group: tanagra.Group;
  criteria: tanagra.Criteria;
}) {
  const underlay = useUnderlay();
  const cohort = useCohort();
  const dispatch = useAppDispatch();

  const [renameDialog, showRenameCriteria] = useTextInputDialog({
    title: "Edit Criteria Name",
    initialText: props.criteria.name,
    textLabel: "Criteria Name",
    buttonLabel: "Confirm",
    onConfirm: (name: string) => {
      dispatch(
        renameCriteria({
          cohortId: cohort.id,
          groupId: props.group.id,
          criteriaId: props.criteria.id,
          criteriaName: name,
        })
      );
    },
  });

  const [menu, show] = useMenu({
    children: [
      <MenuItem
        key="1"
        onClick={() => {
          dispatch(
            deleteCriteria({
              cohortId: cohort.id,
              groupId: props.group.id,
              criteriaId: props.criteria.id,
            })
          );
        }}
      >
        Delete Criteria
      </MenuItem>,
      <MenuItem key="2" onClick={showRenameCriteria}>
        Edit Criteria Name
      </MenuItem>,
    ],
  });

  return (
    <Grid container>
      <Grid item xs="auto">
        <IconButton onClick={show} component="span" size="small">
          <MoreVertIcon fontSize="small" />
        </IconButton>
        {menu}
        {renameDialog}
      </Grid>
      <Grid item xs>
        <Accordion
          disableGutters={true}
          square={true}
          sx={{ boxShadow: 0 }}
          className="criteria-accordion"
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Link
              variant="h6"
              color="inherit"
              underline="hover"
              component={RouterLink}
              to={createUrl({
                underlayName: underlay.name,
                cohortId: cohort.id,
                groupId: props.group.id,
                criteriaId: props.criteria.id,
              })}
            >
              {props.criteria.name}
            </Link>
            <Divider orientation="vertical" variant="middle" flexItem />
            <Typography variant="body1">
              {0 /* TODO(tjennison): Fetch from backend. */}
            </Typography>
          </AccordionSummary>
          <AccordionDetails>
            {getCriteriaPlugin(props.criteria).renderDetails()}
          </AccordionDetails>
        </Accordion>
      </Grid>
    </Grid>
  );
}

const barColours = [
  "#003f5c",
  "#2f4b7c",
  "#665191",
  "#a05195",
  "#d45087",
  "#f95d6a",
  "#ff7c43",
  "#ffa600",
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
      <Typography>{chart.title}</Typography>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart
          data={barData}
          margin={{
            top: 10,
            right: 0,
            left: 20,
            bottom: 10,
          }}
          layout="vertical"
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" />
          <YAxis
            dataKey="name"
            type="category"
            width={150}
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
  cohort: tanagra.Cohort;
};

function DemographicCharts({ cohort }: DemographicChartsProps) {
  const underlay = useUnderlay();
  const source = useSource();

  const api = useContext(EntityCountsApiContext);

  const generatePropertyString = (
    property: ChartConfigProperty,
    entityCountStruct: tanagra.EntityCountStruct
  ) => {
    let propertyString = "";
    // TODO(neelismail): Remove property key check once API supports age.
    const entityCountPropertyValue =
      entityCountStruct.definition?.[
        property.key === "age" ? "year_of_birth" : property.key
      ];

    if (entityCountPropertyValue) {
      let value =
        entityCountPropertyValue.int64Val ??
        entityCountPropertyValue.stringVal ??
        entityCountPropertyValue.boolVal;

      if (value) {
        // TODO(neelismail): Remove age handling once the API supports them.
        if (property.key === "age" && typeof value === "number") {
          value = new Date().getFullYear() - value;
        }

        const definedValue = value;
        if (property.buckets) {
          property.buckets.forEach((range) => {
            const min = range.min;
            const max = range.max;
            const displayName = range.displayName;
            if (
              (min && max && min <= definedValue && definedValue < max) ||
              (min && !max && min <= definedValue) ||
              (!min && max && definedValue < max)
            ) {
              propertyString = displayName;
            }
          });
        } else {
          propertyString = value.toString();
        }
      }
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
          // TODO(neelismail): Remove guard for age property key when API provides age support
          if (
            !additionalSelectedAttributes.has(property.key) &&
            (!groupByAttributes.includes(property.key) ||
              (property.key === "age" &&
                !groupByAttributes.includes("year_of_birth")))
          ) {
            const propertyName =
              property.key === "age" ? "year_of_birth" : property.key;
            additionalSelectedAttributes.add(propertyName);
          }
        });

        // TODO(neelismail): Remove guard for age property key when API provides age support
        if (
          config.stackedProperty &&
          !additionalSelectedAttributes.has(config.stackedProperty.key) &&
          (!groupByAttributes.includes(config.stackedProperty.key) ||
            (config.stackedProperty.key === "age" &&
              !groupByAttributes.includes("year_of_birth")))
        ) {
          const propertyName =
            config.stackedProperty.key === "age"
              ? "year_of_birth"
              : config.stackedProperty.key;
          additionalSelectedAttributes.add(propertyName);
        }
      }
    );

    const searchEntityCountsRequest: tanagra.SearchEntityCountsRequest = {
      entityCounts: {
        entityVariable: "p",
        additionalSelectedAttributes: Array.from(additionalSelectedAttributes),
        groupByAttributes: groupByAttributes,
        filter: generateQueryFilter(source, cohort, "p"),
      },
    };

    const data = await api.searchEntityCounts({
      underlayName: underlay.name,
      entityName: "person",
      searchEntityCountsRequest: searchEntityCountsRequest,
    });

    if (!data.counts) {
      throw new Error(
        "The counts property returned by the searchEntityCounts API is undefined."
      );
    }

    const demographicData = data.counts;
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

  return (
    <>
      <Loading status={demographicState}>
        <Grid item xs={1}>
          <Stack>
            <Typography variant="h4">{`Total Count: ${demographicState.data?.totalCount.toLocaleString()}`}</Typography>
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
        </Grid>
      </Loading>
    </>
  );
}
