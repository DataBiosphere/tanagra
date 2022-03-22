import { configureStore } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";
import conceptSetsReducer from "conceptSetsSlice";
import underlaysReducer from "underlaysSlice";

export const store = configureStore({
  reducer: {
    cohorts: cohortsReducer,
    underlays: underlaysReducer,
    conceptSets: conceptSetsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
