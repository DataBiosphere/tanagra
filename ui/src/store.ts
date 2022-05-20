import { configureStore } from "@reduxjs/toolkit";
import { rootReducer } from "rootReducer";
import { storeUserData } from "storage/storage";

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(storeUserData),
});

export type AppDispatch = typeof store.dispatch;
