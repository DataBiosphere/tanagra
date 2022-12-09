import { createSlice } from "@reduxjs/toolkit";
import { getCurrentUrl } from "./router";

const initialState = "/";
const cohortURLRegex = /.+\/cohorts\/[a-zA-Z0-9]+\/[a-zA-Z0-9]+/;
const urlsSlice = createSlice({
  name: "urlsSlice",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder.addDefaultCase(() => {
      // Force all undo/redo actions within a cohort to refer to the overview
      // page. This is less confusing than unexpectedly navigating to individual
      // pages in a flow (e.g. the add page) because what's changed is actually
      // visible.
      // TODO(tjennison): Configure this behavior as part of routing.
      const currentURL = getCurrentUrl();
      const matches = currentURL.match(cohortURLRegex);
      return !!matches ? matches[0] : currentURL;
    });
  },
});

export default urlsSlice.reducer;
