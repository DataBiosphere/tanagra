import { configureStore } from "@reduxjs/toolkit";
import { rootReducer } from "rootReducer";
import { storeUserData } from "storage/storage";

export function createStore() {
  return configureStore({
    reducer: rootReducer,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(storeUserData),
  });
}

export const store = createStore();

export type AppDispatch = typeof store.dispatch;
