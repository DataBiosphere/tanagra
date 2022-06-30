import { AnyAction, createSlice } from "@reduxjs/toolkit";
import { getCurrentPageUrl } from "./router";

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
      return getCurrentPageUrl();
    });
  },
});

export default urlsSlice.reducer;
