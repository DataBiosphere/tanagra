import {
  AnnotationsApiContext,
  CohortsApiContext,
  ConceptSetsApiContext,
  EntitiesApiContext,
  EntityInstancesApiContext,
  ExportApiContext,
  HintsApiContext,
  ReviewsApiContext,
  StudiesApiContext,
  UnderlaysApiContext,
} from "apiContext";
import Loading from "components/loading";
import { createContext, useCallback, useContext } from "react";
import { Outlet, useParams } from "react-router-dom";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";
import { BackendSource, Source } from "./source";

type SourceContextData = {
  source: Source;
};

export const SourceContext = createContext<SourceContextData | null>(null);

export function useSource() {
  const context = useContext(SourceContext);
  if (!context) {
    throw new Error("Attempting to use source context when not provided.");
  }
  return context.source;
}

export function SourceContextRoot() {
  const { underlayName } = useParams<{ underlayName: string }>();
  if (!underlayName) {
    throw new Error("Underlay name not in URL when creating data source.");
  }

  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const underlaysApi = useContext(UnderlaysApiContext);
  const entitiesApi = useContext(EntitiesApiContext);
  const instancesApi = useContext(
    EntityInstancesApiContext
  ) as tanagra.InstancesV2Api;
  const hintsApi = useContext(HintsApiContext) as tanagra.HintsV2Api;
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesV2Api;
  const cohortsApi = useContext(CohortsApiContext) as tanagra.CohortsV2Api;
  const conceptSetsApi = useContext(
    ConceptSetsApiContext
  ) as tanagra.ConceptSetsV2Api;
  const reviewsApi = useContext(ReviewsApiContext) as tanagra.ReviewsV2Api;
  const annotationsApi = useContext(
    AnnotationsApiContext
  ) as tanagra.AnnotationsV2Api;
  const exportApi = useContext(ExportApiContext) as tanagra.ExportApi;

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

      const entitiesRes = await entitiesApi.listEntities({
        underlayName,
      });
      if (!entitiesRes?.entities) {
        throw new Error(`No entities in underlay ${underlayName}`);
      }

      const underlay = {
        name: underlayName,
        displayName: apiUnderlay.displayName ?? underlayName,
        primaryEntity: apiUnderlay.primaryEntity,
        entities: entitiesRes.entities,
        uiConfiguration: JSON.parse(apiUnderlay.uiConfiguration),
      };

      return {
        source: new BackendSource(
          instancesApi,
          hintsApi,
          studiesApi,
          cohortsApi,
          conceptSetsApi,
          reviewsApi,
          annotationsApi,
          exportApi,
          underlay,
          underlay.uiConfiguration.dataConfig
        ),
      };
    }, [underlayName])
  );

  return (
    <Loading status={sourceState}>
      <SourceContext.Provider value={sourceState.data ?? null}>
        <Outlet />
      </SourceContext.Provider>
    </Loading>
  );
}
