import {
  getAccessToken,
  getExportApiContext,
  getUnderlaysApiContext,
} from "apiContext";
import Loading from "components/loading";
import { BackendUnderlaySource, UnderlaySource } from "data/source";
import { createContext, useCallback, useContext } from "react";
import { Outlet, useParams } from "react-router-dom";
import { useActivityListener } from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";

type UnderlaySourceContextData = {
  source: UnderlaySource;
};

const UnderlaySourceContext = createContext<UnderlaySourceContextData | null>(
  null
);

export function useUnderlaySource() {
  const context = useContext(UnderlaySourceContext);
  if (!context) {
    throw new Error(
      "Attempting to use underlay source context when not provided."
    );
  }
  return context.source;
}

export function UnderlaySourceContextRoot() {
  const { underlayName } = useParams<{ underlayName: string }>();
  if (!underlayName) {
    throw new Error("Underlay name not in URL when creating data source.");
  }

  useActivityListener();

  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const underlaysApi = useContext(
    getUnderlaysApiContext(getAccessToken())
  ) as tanagra.UnderlaysApi;
  const exportApi = useContext(
    getExportApiContext(getAccessToken())
  ) as tanagra.ExportApi;

  const sourceState = useSWRImmutable(
    { type: "underlay", underlayName },
    useCallback(async () => {
      const apiUnderlay = await underlaysApi.getUnderlay({ underlayName });
      if (!apiUnderlay) {
        throw new Error(`Unknown underlay ${underlayName}.`);
      }

      if (!apiUnderlay.uiConfiguration) {
        throw new Error(`No UI configuration in underlay ${underlayName}.`);
      }

      const entitiesRes = await underlaysApi.listEntities({
        underlayName,
      });
      if (!entitiesRes?.entities) {
        throw new Error(`No entities in underlay ${underlayName}`);
      }

      const underlay = {
        name: underlayName,
        uiConfiguration: JSON.parse(apiUnderlay.uiConfiguration),
        underlayConfig: JSON.parse(
          apiUnderlay.serializedConfiguration.underlay
        ),
        criteriaOccurrences:
          apiUnderlay.serializedConfiguration.criteriaOccurrenceEntityGroups.map(
            (co) => JSON.parse(co)
          ),
        groupItems:
          apiUnderlay.serializedConfiguration.groupItemsEntityGroups.map((gi) =>
            JSON.parse(gi)
          ),
        entities: apiUnderlay.serializedConfiguration.entities.map((e) =>
          JSON.parse(e)
        ),
        criteriaSelectors:
          apiUnderlay.serializedConfiguration.criteriaSelectors?.map((cs) =>
            JSON.parse(cs)
          ) ?? [],
        prepackagedDataFeatures:
          apiUnderlay.serializedConfiguration.prepackagedDataFeatures?.map(
            (df) => JSON.parse(df)
          ) ?? [],
        visualizations:
          apiUnderlay.serializedConfiguration.visualizations?.map((v) =>
            JSON.parse(v)
          ) ?? [],
      };

      return {
        source: new BackendUnderlaySource(underlaysApi, exportApi, underlay),
      };
    }, [underlayName])
  );

  return (
    <Loading status={sourceState}>
      <UnderlaySourceContext.Provider value={sourceState.data ?? null}>
        <Outlet />
      </UnderlaySourceContext.Provider>
    </Loading>
  );
}
