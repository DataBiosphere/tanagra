import Typography from "@mui/material/Typography";
import Loading from "components/loading";
import { useSource } from "data/source";
import { useStudyId } from "hooks";
import useSWR from "swr";

export function StudyName() {
  const source = useSource();
  const studyId = useStudyId();

  const studyState = useSWR({ type: "Study", studyId }, async () => {
    return await source.getStudy(studyId);
  });

  return (
    <Loading status={studyState} size="small">
      <Typography variant="body1">
        Study: {studyState.data?.displayName}
      </Typography>
    </Loading>
  );
}
