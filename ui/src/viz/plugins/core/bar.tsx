import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import { useTheme } from "@mui/material/styles";
import Typography from "@mui/material/Typography";
import { useMemo } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Text,
  Tooltip,
  TooltipProps,
  XAxis,
  YAxis,
} from "recharts";
import { isValid } from "util/valid";
import {
  registerVizPlugin,
  VizData,
  VizKeyType,
  VizPlugin,
  VizValueType,
} from "viz/viz";

interface Config {
  colors?: string[];
}

const defaultColors = [
  "#4450C0",
  "#F7963F",
  "#4393C3",
  "#FBCD50",
  "#53978B",
  "#D14545",
  "#538B61",
  "#D77CA8",
  "#B6D07E",
  "#91C3C7",
  "#1F255C",
  "#9D4D07",
  "#1D455D",
  "#B48504",
  "#335C55",
  "#832121",
  "#538B61",
  "#AC356E",
  "#448388",
];

@registerVizPlugin("core/bar", [
  {
    keyTypes: [[VizKeyType.NumericId, VizKeyType.StringId]],
    valueTypes: [VizValueType.Numeric],
  },
  {
    keyTypes: [
      [VizKeyType.NumericId, VizKeyType.StringId],
      [VizKeyType.NumericId, VizKeyType.StringId],
    ],
    valueTypes: [VizValueType.Numeric],
  },
])
class _ implements VizPlugin {
  config: Config;

  constructor(config: object) {
    this.config = config as Config;
  }

  render(data: VizData[]) {
    return <BarViz config={this.config} data={data} />;
  }
}

type BarVizProps = {
  config: Config;
  data: VizData[];
};

export function BarViz(props: BarVizProps) {
  const theme = useTheme();

  const stackedProperties = useMemo(
    () =>
      Array.from(new Set(props.data.map((d) => d.keys[1]?.name))).filter(
        isValid
      ),
    [props.data]
  );

  const barData = useMemo(() => {
    const barData: Record<string, Record<string, string | number>> = {};
    props.data.forEach((d) => {
      if (!barData[d.keys[0].name]) {
        barData[d.keys[0].name] = { name: d.keys[0].name };
      }

      const bd = barData[d.keys[0].name];
      const n = d.values[0].numeric ?? 0;
      if (d.keys.length > 1) {
        bd[d.keys[1].name] = n;
      } else {
        bd.count = n;
      }
    });

    const arr: Record<string, string | number>[] = [];
    for (const k in barData) {
      arr.push({
        name: k,
        ...barData[k],
      });
    }
    return arr;
  }, [props.data]);

  const barColors = props.config.colors ?? defaultColors;

  return (
    <>
      <ResponsiveContainer>
        <BarChart data={barData} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            type="number"
            height={20}
            style={{
              ...theme.typography.body2,
            }}
          />
          <YAxis
            dataKey="name"
            type="category"
            interval={0}
            width={160}
            tick={(props) => (
              <Text {...props} maxLines={1} width={156}>
                {props.payload.value}
              </Text>
            )}
            style={{
              ...theme.typography.body2,
            }}
          />
          <Tooltip
            content={(props: TooltipProps<number, string>) => {
              return (
                <Paper elevation={1} sx={{ p: 1 }}>
                  <Stack>
                    <Typography variant="body2em">{props.label}</Typography>
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
                        <Typography variant="body2">
                          {(stackedProperties.length > 0
                            ? row.name + ": "
                            : "") + row.value}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                </Paper>
              );
            }}
          />
          {stackedProperties.length > 0 ? (
            stackedProperties.map((property, index) => (
              <Bar
                key={index}
                dataKey={property as string}
                stackId="a"
                fill={barColors[index % barColors.length]}
                maxBarSize={100}
              />
            ))
          ) : (
            <Bar dataKey="count" fill={barColors[0]} maxBarSize={60} />
          )}
        </BarChart>
      </ResponsiveContainer>
    </>
  );
}
