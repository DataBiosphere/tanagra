import Typography from "@mui/material/Typography";
import Loading from "components/loading";
import { useStudySource } from "data/studySourceContext";
import { useStudyId, useUnderlay } from "hooks";
import useSWR from "swr";

export function StudyName() {
  const studySource = useStudySource();
  const studyId = useStudyId();
  const underlay = useUnderlay();

  const studyState = useSWR({ type: "Study", studyId }, async () => {
    return await studySource.getStudy(studyId);
  });

  return (
    <Loading status={studyState} size="small">
      <Typography variant="body1">
        {underlay.uiConfiguration.featureConfig?.renameStudyLabel ?? "Study"}:{" "}
        {studyState.data?.displayName}
      </Typography>
    </Loading>
  );
}
