import { CohortContext, useNewCohortContext } from "cohortContext";
import Loading from "components/loading";
import { Outlet } from "react-router-dom";

export default function CohortRoot() {
  const status = useNewCohortContext();

  return (
    <Loading status={status}>
      <CohortContext.Provider value={status.context}>
        <Outlet />
      </CohortContext.Provider>
    </Loading>
  );
}
