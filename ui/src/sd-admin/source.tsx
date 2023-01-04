import {
  CohortsApiContext,
  ConceptSetsApiContext,
  ReviewsApiContext,
  StudiesApiContext,
} from "apiContext";
import { useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";
import {
  CohortV2,
  ConceptSetV2,
  CreateCohortRequest,
  CreateStudyRequest,
  CriteriaGroupV2,
  ReviewV2,
  StudyV2,
  UpdateCohortRequest,
  UpdateStudyRequest,
} from "tanagra-api";

export interface AdminSource {
  createStudy(
    displayName: string,
    description: string,
    properties: Array<object>
  ): Promise<StudyV2>;

  updateStudy(
    studyId: string,
    displayName: string,
    description: string
  ): Promise<StudyV2>;

  getStudiesList(): Promise<StudyV2[]>;

  createCohort(
    studyId: string,
    displayName: string,
    description: string,
    underlayName: string
  ): Promise<CohortV2>;

  updateCohort(
    studyId: string,
    cohortId: string,
    displayName: string,
    description: string,
    criteriaGroups: Array<CriteriaGroupV2>
  ): Promise<CohortV2>;

  getCohortsForStudy(studyId: string): Promise<CohortV2[]>;

  getConceptSetsForStudy(studyId: string): Promise<ConceptSetV2[]>;

  getReviewsForStudy(studyId: string, cohortId: string): Promise<ReviewV2[]>;
}

export function useAdminSource(): AdminSource {
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesV2Api;
  const cohortsApi = useContext(CohortsApiContext) as tanagra.CohortsV2Api;
  const conceptSetsApi = useContext(
    ConceptSetsApiContext
  ) as tanagra.ConceptSetsV2Api;
  const reviewsApi = useContext(ReviewsApiContext) as tanagra.ReviewsV2Api;
  return useMemo(
    () =>
      new BackendAdminSource(
        studiesApi,
        cohortsApi,
        conceptSetsApi,
        reviewsApi
      ),
    []
  );
}

export class BackendAdminSource implements AdminSource {
  constructor(
    private studiesApi: tanagra.StudiesV2Api,
    private cohortsApi: tanagra.CohortsV2Api,
    private conceptSetsApi: tanagra.ConceptSetsV2Api,
    private reviewsApi: tanagra.ReviewsV2Api
  ) {}

  async createStudy(
    displayName: string,
    description: string,
    properties: Array<object>
  ): Promise<StudyV2> {
    const createStudyRequest: CreateStudyRequest = {
      studyCreateInfoV2: { displayName, description, properties },
    };
    return await this.studiesApi.createStudy(createStudyRequest);
  }

  async updateStudy(
    studyId: string,
    displayName: string,
    description: string
  ): Promise<StudyV2> {
    const updateStudyRequest: UpdateStudyRequest = {
      studyId,
      studyUpdateInfoV2: {
        displayName,
        description,
      },
    };
    return await this.studiesApi.updateStudy(updateStudyRequest);
  }

  async getStudiesList(): Promise<StudyV2[]> {
    return await this.studiesApi.listStudies({});
  }

  async createCohort(
    studyId: string,
    displayName: string,
    description: string,
    underlayName: string
  ): Promise<CohortV2> {
    const createCohortRequest: CreateCohortRequest = {
      studyId,
      cohortCreateInfoV2: {
        displayName,
        description,
        underlayName,
      },
    };
    return await this.cohortsApi.createCohort(createCohortRequest);
  }

  async updateCohort(
    studyId: string,
    cohortId: string,
    displayName: string,
    description: string,
    criteriaGroups: Array<CriteriaGroupV2>
  ): Promise<CohortV2> {
    const updateCohortRequest: UpdateCohortRequest = {
      studyId,
      cohortId,
      cohortUpdateInfoV2: {
        displayName,
        description,
        criteriaGroups,
      },
    };
    return await this.cohortsApi.updateCohort(updateCohortRequest);
  }

  async getCohortsForStudy(studyId: string): Promise<CohortV2[]> {
    return await this.cohortsApi.listCohorts({ studyId });
  }

  async getConceptSetsForStudy(studyId: string): Promise<ConceptSetV2[]> {
    return await this.conceptSetsApi.listConceptSets({ studyId });
  }

  async getReviewsForStudy(
    studyId: string,
    cohortId: string
  ): Promise<ReviewV2[]> {
    return await this.reviewsApi.listReviews({ studyId, cohortId });
  }
}
