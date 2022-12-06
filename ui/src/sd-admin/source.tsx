import { StudiesApiContext } from "apiContext";
import { useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";
import { CreateStudyRequest, StudyV2, UpdateStudyRequest } from "tanagra-api";

export interface AdminSource {
  createStudy(createStudyRequest: CreateStudyRequest): Promise<StudyV2>;

  updateStudy(updateStudyRequest: UpdateStudyRequest): Promise<StudyV2>;

  getStudiesList(): Promise<StudyV2[]>;
}

export function useAdminSource(): AdminSource {
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesV2Api;
  return useMemo(() => new BackendAdminSource(studiesApi), []);
}

export class BackendAdminSource implements AdminSource {
  constructor(private studiesApi: tanagra.StudiesV2Api) {}

  async createStudy(createStudyRequest: CreateStudyRequest): Promise<StudyV2> {
    return await this.studiesApi.createStudy(createStudyRequest);
  }

  async updateStudy(updateStudyRequest: UpdateStudyRequest): Promise<StudyV2> {
    return await this.studiesApi.updateStudy(updateStudyRequest);
  }

  async getStudiesList(): Promise<StudyV2[]> {
    return await this.studiesApi.listStudies({});
  }
}
