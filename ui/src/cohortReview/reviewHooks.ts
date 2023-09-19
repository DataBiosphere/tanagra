import { useCohortContext } from "cohortContext";
import { useSource } from "data/sourceContext";
import { useUnderlay } from "hooks";
import { useMemo } from "react";
import { useParams } from "react-router-dom";
import { useBaseParams } from "router";
import useSWR from "swr";
import * as tanagraUI from "tanagra-ui";
import { CohortReviewConfig } from "underlaysSlice";
import { useSearchData } from "util/searchData";

export type ReviewParams = {
  studyId: string;
  reviewId: string;
  cohort: tanagraUI.UICohort;

  primaryKey: string;
  primaryAttributes: string[];
  uiConfig: CohortReviewConfig;
};

export function useReviewParams(): ReviewParams {
  const underlay = useUnderlay();
  const params = useBaseParams();

  const primaryKey = underlay.uiConfiguration.dataConfig.primaryEntity.key;
  const uiConfig = underlay.uiConfiguration.cohortReviewConfig;

  const primaryAttributes = useMemo(
    () => [primaryKey, ...uiConfig.attributes.map((a) => a.key)],
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
  const source = useSource();
  const params = useReviewParams();

  return useSWR(
    {
      type: "reviewInstance",
      studyId: params.studyId,
      reviewId: params.reviewId,
      cohortId: params.cohort.id,
    },
    async (key) => {
      return await source.listReviewInstances(
        key.studyId,
        key.cohortId,
        key.reviewId,
        params.primaryAttributes
      );
    }
  );
}

export function useReviewAnnotations() {
  const source = useSource();
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
      return await source.listAnnotations(key.studyId, key.cohortId);
    }
  );
}

export type PluginSearchData = {
  [x: string]: object;
};

export type SearchData = {
  instanceIndex?: number;
  pageId?: string;
  editingAnnotations?: boolean;

  plugins?: PluginSearchData;
};

export function useReviewSearchData() {
  return useSearchData<SearchData>();
}
