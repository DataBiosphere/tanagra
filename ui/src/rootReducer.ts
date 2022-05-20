import { combineReducers, createAction } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";
import conceptSetsReducer from "conceptSetsSlice";
import { AnyAction, Reducer } from "redux";
import * as tanagra from "tanagra-api";
import underlaysReducer from "underlaysSlice";

const slicesReducer = combineReducers({
  cohorts: cohortsReducer,
  underlays: underlaysReducer,
  conceptSets: conceptSetsReducer,
});
export type RootState = ReturnType<typeof slicesReducer>;

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  if (loadUserData.match(action)) {
    return {
      ...state,
      cohorts: action.payload.cohorts,
      conceptSets: action.payload.conceptSets,
    };
  }
  return slicesReducer(state, action);
};
