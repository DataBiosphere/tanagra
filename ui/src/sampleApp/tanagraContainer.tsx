import GridLayout from "layout/gridLayout";
import { useHref, useParams } from "react-router-dom";
import {
  RETURN_URL_PLACEHOLDER,
  useBaseParams,
  useExitActionListener,
  useRedirectListener,
} from "router";
import { Header } from "sampleApp/header";
import { useNavigate } from "util/searchState";

export function TanagraContainer() {
  const navigate = useNavigate();
  const params = useBaseParams();
  const { "*": splat } = useParams<{ "*": string }>();

  const currentPath = useHref(".");

  useExitActionListener(() => {
    navigate(`/underlays/${params.underlayName}/studies/${params.studyId}`);
  });

  useRedirectListener((redirectUrl, returnPath) => {
    const currentURL = new URL(window.location.href);
    const returnUrl = encodeURIComponent(
      `${currentURL.origin}/${currentPath}${returnPath}`
    );
    window.location.href = redirectUrl.replace(
      RETURN_URL_PLACEHOLDER,
      returnUrl
    );
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
