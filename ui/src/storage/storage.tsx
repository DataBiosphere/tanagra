import Loading from "components/loading";
import { useAppDispatch } from "hooks";
import { useCallback } from "react";
import { useAsync } from "react-async";
import { AnyAction, Dispatch, Middleware, MiddlewareAPI } from "redux";
import { loadUserData, RootState } from "rootReducer";
import * as tanagra from "tanagra-api";

const currentVersion = 2;

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

export function LoadingUserData(props: { children?: React.ReactNode }) {
  const dispatch = useAppDispatch();
  const status = useAsync<tanagra.UserData>(
    useCallback(() => {
      if (!storagePlugin) {
        throw new Error("No storage plugin configured.");
      }

      return storagePlugin.load().then((data?: tanagra.UserData) => {
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

        dispatch(loadUserData(data));
      });
    }, [])
  );

  return (
    <Loading status={status}>
      <>{props.children}</>
    </Loading>
  );
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
