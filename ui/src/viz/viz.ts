import { useMemo } from "react";

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

export function getVizPlugin(type: string, config: object): VizPlugin {
  return new (getVizEntry(type).constructor)(config);
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
