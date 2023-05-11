import { AnyAction, Dispatch, Middleware, MiddlewareAPI } from "redux";
import { loadUserData, RootState } from "rootReducer";
import { AppDispatch } from "store";
import * as tanagra from "tanagra-api";
import { Underlay } from "underlaysSlice";

const currentVersion = 7;

export interface StoragePlugin {
  store(data: tanagra.UserData): Promise<void>;
  load(): Promise<tanagra.UserData | undefined>;
}

export const storeUserData: Middleware<unknown, RootState> =
  (store: MiddlewareAPI<Dispatch<AnyAction>>) =>
  (next: Dispatch<AnyAction>) =>
  (action: AnyAction): AnyAction => {
    const result = next(action);
    if (storagePlugin) {
      const state = store.getState();

      const data: tanagra.UserData = {
        version: currentVersion,
        cohorts: state.present.cohorts,
        conceptSets: state.present.conceptSets,
      };

      storagePlugin.store(data);
    }
    return result;
  };

export async function fetchUserData(
  dispatch: AppDispatch,
  underlays: Underlay[]
) {
  if (!storagePlugin) {
    throw new Error("No storage plugin configured.");
  }

  const data = await storagePlugin.load();
  if (!data) {
    return;
  }

  if (data.version > currentVersion) {
    throw new Error(
      `Unable to load newer data: verson ${data.version} > current version ${currentVersion}`
    );
  }

  // TODO(tjennison): Handle backwards compatability when we're closer to
  // launch.
  if (data.version != currentVersion) {
    data.cohorts = [];
    data.conceptSets = [];
  }

  data.cohorts = data.cohorts || [];
  data.conceptSets = data.conceptSets || [];

  // TODO(tjennison): Add versioning to criteria.
  for (const cohort of data.cohorts) {
    const underlay = underlays.find((u) => u.name === cohort.underlayName);
    if (!underlay) {
      continue;
    }

    for (const section of cohort.groupSections) {
      for (const group of section.groups) {
        for (const criteria of group.criteria) {
          const config = underlay.uiConfiguration.criteriaConfigs.find(
            (config) => config.id === criteria.config.id
          );
          if (!config) {
            throw new Error(
              `Underlay has no support for criteria "${criteria.config.id}".`
            );
          }
          criteria.config = config;
        }
      }
    }
  }

  dispatch(loadUserData(data));
}

// registerStoragePlugin is a decorator that allows a storage plugin to be used
// simply by importing it.
export function registerStoragePlugin() {
  return <T extends StoragePluginConstructor>(constructor: T): void => {
    storagePlugin = new constructor();
  };
}

interface StoragePluginConstructor {
  new (): StoragePlugin;
}

let storagePlugin: StoragePlugin | null = null;
