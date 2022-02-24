import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import * as tanagra from "tanagra-api";

export type Underlay = {
  name: string;
  entities: tanagra.Entity[];
};

const initialState: Underlay[] = [];

const underlaysSlice = createSlice({
  name: "underlays",
  initialState,
  reducers: {
    setUnderlays: (state, action: PayloadAction<Underlay[]>) => {
      return action.payload;
    },
  },
});

export const { setUnderlays } = underlaysSlice.actions;

export default underlaysSlice.reducer;
