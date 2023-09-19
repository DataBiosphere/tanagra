import {
  AnnotationsApiContext,
  CohortsApiContext,
  ConceptSetsApiContext,
  ExportApiContext,
  ReviewsApiContext,
  StudiesApiContext,
  UnderlaysApiContext,
  UsersApiContext,
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
  const underlaysApi = useContext(UnderlaysApiContext) as tanagra.UnderlaysApi;
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesApi;
  const cohortsApi = useContext(CohortsApiContext) as tanagra.CohortsApi;
  const conceptSetsApi = useContext(
    ConceptSetsApiContext
  ) as tanagra.ConceptSetsApi;
  const reviewsApi = useContext(ReviewsApiContext) as tanagra.ReviewsApi;
  const annotationsApi = useContext(
    AnnotationsApiContext
  ) as tanagra.AnnotationsApi;
  const exportApi = useContext(ExportApiContext) as tanagra.ExportApi;
  const usersApi = useContext(UsersApiContext) as tanagra.UsersApi;

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
        displayName: apiUnderlay.displayName ?? underlayName,
        primaryEntity: apiUnderlay.primaryEntity,
        entities: entitiesRes.entities,
        uiConfiguration: JSON.parse(apiUnderlay.uiConfiguration),
      };

      return {
        source: new BackendSource(
          underlaysApi,
          studiesApi,
          cohortsApi,
          conceptSetsApi,
          reviewsApi,
          annotationsApi,
          exportApi,
          usersApi,
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
