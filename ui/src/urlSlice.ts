import { createSlice } from "@reduxjs/toolkit";
import { getCurrentUrl } from "./router";

const initialState = "/";

const urlsSlice = createSlice({
  name: "urlsSlice",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder.addDefaultCase(() => {
      return getCurrentUrl();
    });
  },
});

export default urlsSlice.reducer;
