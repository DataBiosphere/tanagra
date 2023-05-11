import { configureStore } from "@reduxjs/toolkit";
import { rootReducer } from "rootReducer";

export function createStore() {
  return configureStore({
    reducer: rootReducer,
  });
}

export const store = createStore();

export type AppDispatch = typeof store.dispatch;
