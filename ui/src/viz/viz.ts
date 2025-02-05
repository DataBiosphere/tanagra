import { VALUE_SUFFIX } from "data/configuration";
import { FilterCountValue } from "data/source";
import { compareDataValues } from "data/types";
import * as vizProto from "proto/viz/viz_data_config";
import { useMemo } from "react";
import { isValid } from "util/valid";

// The data format shared across all visualizations is 1+ keys and 1+ values.
// The keys have both names and values in preparation to support interactive
// charts. Multiple keys support cases like stacked bar charts, while multiple
// values support cases like scatterplots. Each plugin returns a list of
// supported data formats so they will be able to know what input data they can
// be used with.
export type VizKey = {
  name: string;

  numericId?: number;
  stringId?: string;
};

export type VizValue = {
  numeric?: number;
  quartiles?: number[];
};

export type VizData = {
  keys: VizKey[];
  values: VizValue[];
};

export type VizPlugin = {
  render: (data: VizData[]) => JSX.Element;
};

export enum VizKeyType {
  NumericId = "numeric",
  StringId = "stringId",
}

export enum VizValueType {
  Numeric = "numeric",
  Quartiles = "quartiles",
}

export type VizDataFormat = {
  keyTypes: VizKeyType[][];
  valueTypes: VizValueType[];
};

export function processFilterCountValues(
  vizDataConfig: vizProto.VizDataConfig,
  fcvs: FilterCountValue[]
): VizData[] {
  const vizSource = vizDataConfig.sources[0];

  const dataMap = new Map<string, VizData>();
  fcvs.forEach((d) => {
    let exclude = false;
    const vd: VizData = {
      keys: vizSource.attributes.map((a) => {
        let value = d[a.attribute];
        if (!isValid(value)) {
          value = "Unknown";
        }
        if (typeof value === "bigint") {
          value = Number(value);
        }

        let name: string | undefined = undefined;
        let numericId: number | undefined = undefined;
        let stringId: string | undefined = undefined;
        if (a.numericBucketing && typeof value === "number") {
          const thresholds = a.numericBucketing.thresholds ?? [];
          if (!thresholds.length) {
            const intervals = a.numericBucketing.intervals;
            if (!intervals || !intervals.min || !intervals.max) {
              throw new Error(
                "Bucketing is configured without thresholds or intervals."
              );
            }

            for (let i = 0; i < intervals.count + 1n; i++) {
              thresholds.push(
                intervals.min + i * (intervals.max - intervals.min)
              );
            }
          }

          if (a.numericBucketing.includeLesser && value < thresholds[0]) {
            name = `<${thresholds[0]}`;
            numericId = 0;
          } else {
            for (let i = 1; i < thresholds.length; i++) {
              if (value >= thresholds[i - 1] && value < thresholds[i]) {
                name = `${thresholds[i - 1]}-${thresholds[i]}`;
                numericId = i;
                break;
              }
            }
          }
          if (a.numericBucketing.includeGreater && !name) {
            name = `>=${thresholds[thresholds.length - 1]}`;
            numericId = thresholds.length;
          }

          if (!name) {
            exclude = true;
            return { name: "" };
          }
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
      if (!attrib.numericBucketing) {
        if (
          !attrib.sortType ||
          attrib.sortType ===
            vizProto.VizDataConfig_Source_Attribute_SortType.NAME
        ) {
          sortValue = compareDataValues(a.keys[i].name, b.keys[i].name);
        } else if (
          attrib.sortType ===
          vizProto.VizDataConfig_Source_Attribute_SortType.VALUE
        ) {
          sortValue = compareDataValues(
            a.values[i].numeric,
            b.values[i].numeric
          );
        }
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
    let count = 0n;
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

// registerVizPlugin is a decorator that allows visualizations to automatically
// register with the app simply by importing them.
export function registerVizPlugin(type: string, dataFormats: VizDataFormat[]) {
  return <T extends VizPluginConstructor>(constructor: T): void => {
    vizRegistry.set(type, {
      dataFormats,
      constructor,
    });
  };
}

export function getVizPlugin(type: string, config?: object): VizPlugin {
  return new (getVizEntry(type).constructor)(config ?? {});
}

export function useVizPlugin(type: string, config: object): VizPlugin {
  return useMemo(() => getVizPlugin(type, config), [type, config]);
}

function getVizEntry(type: string): RegistryEntry {
  const entry = vizRegistry.get(type);
  if (!entry) {
    throw `Unknown viz plugin type '${type}'`;
  }
  return entry;
}

interface VizPluginConstructor {
  new (config: object): VizPlugin;
}

type RegistryEntry = {
  dataFormats: VizDataFormat[];
  constructor: VizPluginConstructor;
};

const vizRegistry = new Map<string, RegistryEntry>();
