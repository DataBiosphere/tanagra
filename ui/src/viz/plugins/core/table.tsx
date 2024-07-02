import { TreeGrid, useArrayAsTreeGridData } from "components/treegrid";
import { GridBox } from "layout/gridBox";
import { useMemo } from "react";
import {
  registerVizPlugin,
  VizData,
  VizKeyType,
  VizPlugin,
  VizValueType,
} from "viz/viz";

interface Config {
  keyTitles?: string[];
  valueTitles?: string[];
}

@registerVizPlugin("core/table", [
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
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements VizPlugin {
  config: Config;

  constructor(config: object) {
    this.config = config as Config;
  }

  render(data: VizData[]) {
    return <TableViz config={this.config} data={data} />;
  }
}

type TableVizProps = {
  config: Config;
  data: VizData[];
};

function TableViz(props: TableVizProps) {
  const data = useMemo(
    () =>
      props.data.map((d) => {
        const row: { [x: string]: string | number | undefined } = {
          id: d.keys.map((k) => k.numericId ?? k.stringId).join("~"),
        };

        d.keys.forEach((k, i) => (row["k" + i] = k.name));
        d.values.forEach((v, i) => (row["v" + i] = v.numeric));

        return row;
      }),
    [props.data]
  );

  const treeGridData = useArrayAsTreeGridData(data, "id");

  const columns = useMemo(() => {
    const d0 = props.data[0];
    const width = 100 / (d0.keys.length + d0.values.length) + "%";
    return [
      ...d0.keys.map((k, i) => ({
        key: "k" + i,
        width,
        title: props.config.keyTitles?.[i] ?? "Key " + i,
      })),
      ...d0.values.map((v, i) => ({
        key: "v" + i,
        width,
        title: props.config.valueTitles?.[i] ?? "Count",
      })),
    ];
  }, [props.data]);

  return (
    <GridBox sx={{ pb: 1, overflowY: "auto", maxHeight: "300px" }}>
      <TreeGrid data={treeGridData} columns={columns} padding={0} />
    </GridBox>
  );
}
