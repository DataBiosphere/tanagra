import { useCohortContext } from "cohortContext";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useUnderlay } from "hooks";
import { useMemo } from "react";
import { useParams } from "react-router-dom";
import { useBaseParams } from "router";
import useSWR from "swr";
import * as tanagraUI from "tanagra-ui";
import { CohortReviewConfig } from "underlaysSlice";
import { useLocalSearchState } from "util/searchState";

export type ReviewParams = {
  studyId: string;
  reviewId: string;
  cohort: tanagraUI.UICohort;

  primaryKey: string;
  primaryAttributes: string[];
  uiConfig: CohortReviewConfig;
};

export function useReviewParams(): ReviewParams {
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const params = useBaseParams();

  const primaryKey = underlaySource.primaryEntity().idAttribute;
  const uiConfig = underlay.uiConfiguration.cohortReviewConfig;
  const participantIdAttribute = uiConfig.participantIdAttribute ?? primaryKey;

  const primaryAttributes = useMemo(
    () => [
      primaryKey,
      participantIdAttribute,
      ...uiConfig.attributes.map((a) => a.key),
    ],
    [uiConfig]
  );

  const { reviewId } = useParams<{ reviewId: string }>();
  if (!reviewId) {
    throw new Error("Review ID is null.");
  }

  const cohort = useCohortContext().state?.present;
  if (!cohort) {
    throw new Error("Cohort context state is null.");
  }

  return {
    studyId: params.studyId,
    reviewId,
    cohort,
    primaryKey,
    primaryAttributes,
    uiConfig,
  };
}

export function useReviewInstances() {
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const params = useReviewParams();

  return useSWR(
    {
      type: "reviewInstance",
      studyId: params.studyId,
      reviewId: params.reviewId,
      cohortId: params.cohort.id,
    },
    async (key) => {
      return await studySource.listReviewInstances(
        key.studyId,
        underlaySource,
        key.cohortId,
        key.reviewId,
        params.primaryAttributes
      );
    }
  );
}

export function useReviewAnnotations() {
  const studySource = useStudySource();
  const params = useBaseParams();

  const cohort = useCohortContext().state?.present;
  if (!cohort) {
    throw new Error("Cohort context state is null.");
  }

  return useSWR(
    {
      type: "annotation",
      studyId: params.studyId,
      cohortId: cohort.id,
    },
    async (key) => {
      return await studySource.listAnnotations(key.studyId, key.cohortId);
    }
  );
}

export type PluginSearchState = {
  [x: string]: object;
};

export type SearchState = {
  instanceIndex?: number;
  pageId?: string;

  plugins?: PluginSearchState;
};

export function useReviewSearchState() {
  return useLocalSearchState<SearchState>();
}
