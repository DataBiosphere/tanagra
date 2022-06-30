import { AnyAction, createSlice } from "@reduxjs/toolkit";

const createUrlParams = (): string => {
  const baseUrl = "http://localhost:3000/#/"; // TODO: find solution for URL
  const urlPath = window.location.href.slice(baseUrl.length);
  return urlPath;
};

function isUndoableAction(action: AnyAction): boolean {
  return (
    action.type.startsWith("cohorts") || action.type.startsWith("conceptSets")
  );
}

const initialState = "/";

const urlsSlice = createSlice({
  name: "urlsSlice",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder.addMatcher(isUndoableAction, () => {
      return createUrlParams();
    });
  },
});

export default urlsSlice.reducer;
