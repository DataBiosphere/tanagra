import { ReactNode } from "react";
import { CohortReviewPageConfig } from "underlaysSlice";

export interface CohortReviewPlugin {
  id: string;
  title: string;
  occurrences: string[];

  render: () => ReactNode;
}

// registerCohortReviewPlugin is a decorator that allows cohort review plugins
// to automatically register with the app simply by importing them.
export function registerCohortReviewPlugin(type: string) {
  return <T extends CohortReviewPluginConstructor>(constructor: T): void => {
    pluginRegistry.set(type, {
      constructor,
    });
  };
}

export function getCohortReviewPlugin(
  config: CohortReviewPageConfig
): CohortReviewPlugin {
  return new (getCohortReviewEntry(config.type).constructor)(
    config.id,
    config.title,
    config
  );
}

function getCohortReviewEntry(type: string): RegistryEntry {
  const entry = pluginRegistry.get(type);
  if (!entry) {
    throw `Unknown plugin type '${type}'`;
  }
  return entry;
}

interface CohortReviewPluginConstructor {
  new (
    id: string,
    title: string,
    config: CohortReviewPageConfig
  ): CohortReviewPlugin;
}

type RegistryEntry = {
  constructor: CohortReviewPluginConstructor;
};

const pluginRegistry = new Map<string, RegistryEntry>();
