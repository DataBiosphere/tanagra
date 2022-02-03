import { configureStore } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";

export const store = configureStore({
  reducer: {
    cohorts: cohortsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
