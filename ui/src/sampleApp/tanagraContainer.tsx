import GridLayout from "layout/gridLayout";
import { useNavigate, useParams } from "react-router-dom";
import { useBaseParams, useExitActionListener } from "router";
import { Header } from "sampleApp/header";

export function TanagraContainer() {
  const navigate = useNavigate();
  const params = useBaseParams();
  const { "*": splat } = useParams<{ "*": string }>();

  useExitActionListener(() => {
    navigate(`/underlays/${params.underlayName}/studies/${params.studyId}`);
  });

  return (
    <GridLayout rows>
      <Header />
      <iframe
        width="100%"
        height="100%"
        frameBorder="0"
        src={"/#/" + splat}
        style={{ display: "block" }}
      />
    </GridLayout>
  );
}
