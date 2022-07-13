import { createSlice } from "@reduxjs/toolkit";
import { getCurrentPageUrl } from "./router";

const initialState = "/";

const urlsSlice = createSlice({
  name: "urlsSlice",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder.addDefaultCase(() => {
      return getCurrentPageUrl();
    });
  },
});

export default urlsSlice.reducer;
