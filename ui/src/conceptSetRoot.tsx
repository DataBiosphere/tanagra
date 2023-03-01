import Loading from "components/loading";
import { ConceptSetContext, useNewConceptSetContext } from "conceptSetContext";
import { Outlet } from "react-router-dom";

export default function ConceptSetRoot() {
  const status = useNewConceptSetContext();

  return (
    <Loading status={status}>
      <ConceptSetContext.Provider value={status.context}>
        <Outlet />
      </ConceptSetContext.Provider>
    </Loading>
  );
}
