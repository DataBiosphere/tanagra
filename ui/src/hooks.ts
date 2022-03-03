import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { AppDispatch, RootState } from "store";

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export const useUnderlay = () => {
  const { underlayName } = useParams<{ underlayName: string }>();
  return useAppSelector((state) =>
    state.underlays.find((underlay) => underlay.name === underlayName)
  );
};

// The useXOrFail versions are generally the correct choice since they can be
// used where the existence of X is enforced by higher level parts of the UI
// (e.g. the underlay should always be valid once past the underlay selection
// page) so the code doesn't have to handle the undefined case.
export const useUnderlayOrFail = () => {
  const underlay = useUnderlay();
  if (!underlay) {
    throw new Error("Unknown underlay.");
  }
  return underlay;
};

export const useCohort = () => {
  const { cohortId } = useParams<{ cohortId: string }>();
  return useAppSelector((state) =>
    state.cohorts.find((cohort) => cohort.id === cohortId)
  );
};

export const useCohortOrFail = () => {
  const cohort = useCohort();
  if (!cohort) {
    throw new Error("Unknown cohort.");
  }
  return cohort;
};
