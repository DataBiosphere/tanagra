import { combineReducers, createAction } from "@reduxjs/toolkit";
import undoable from 'redux-undo';
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

const undoableSlicesReducer = undoable(slicesReducer);

export type RootState = ReturnType<typeof undoableSlicesReducer>;

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  if (loadUserData.match(action)) {
    return {
      ...state,
      present: {
        ...state.present,
        cohorts: action.payload.cohorts,
        conceptSets: action.payload.conceptSets,
      }
    };
  }
  return undoableSlicesReducer(state, action);
};
