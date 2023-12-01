import { CohortSummary } from "cohortSummary";
import Loading from "components/loading";
import { useStudySource } from "data/studySourceContext";
import { useParams } from "react-router-dom";
import useSWRImmutable from "swr/immutable";

export function CohortRevision() {
  const studySource = useStudySource();
  const { studyId, cohortId, revisionId } =
    useParams<{ studyId: string; cohortId: string; revisionId: string }>();

  const cohortState = useSWRImmutable(
    { type: "cohort", studyId, cohortId, revisionId },
    async () =>
      await studySource.getCohort(studyId ?? "", cohortId ?? "", revisionId)
  );

  return (
    <Loading status={cohortState}>
      {cohortState.data ? <CohortSummary cohort={cohortState.data} /> : null}
    </Loading>
  );
}
