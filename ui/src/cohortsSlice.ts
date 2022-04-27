import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Cohort, Criteria, generateId, Group, GroupKind } from "cohort";

const initialState: Cohort[] = [];

// TODO(tjennison): Normalize groups and criteria to simplify a lot of this
// nested code. This may require changing how the slices are arranged though,
// since having cohorts, groups, and criteria in separate slices may end up
// requiring too many special cases that cross slices.
const cohortsSlice = createSlice({
  name: "cohorts",
  initialState,
  reducers: {
    insertCohort: {
      reducer: (state, action: PayloadAction<Cohort>) => {
        state.push(action.payload);
      },
      prepare: (name: string, underlayName: string, attributes: string[]) => ({
        payload: {
          id: generateId(),
          name,
          underlayName,
          attributes,
          groups: [],
        },
      }),
    },

    insertGroup: {
      reducer: (
        state,
        action: PayloadAction<{ cohortId: string; group: Group }>
      ) => {
        const cohort = state.find((c) => c.id === action.payload.cohortId);
        if (cohort) {
          cohort.groups.push(action.payload.group);
        }
      },
      prepare: (cohortId: string, kind: GroupKind, criteria: Criteria) => ({
        payload: {
          cohortId,
          group: {
            id: generateId(),
            kind,
            criteria: [criteria],
          },
        },
      }),
    },

    renameGroup: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        groupName: string;
      }>
    ) => {
      const cohort = state.find((c) => c.id === action.payload.cohortId);
      if (cohort) {
        const group = cohort.groups.find(
          (g) => g.id === action.payload.groupId
        );
        if (group) {
          group.name = action.payload.groupName;
        }
      }
    },

    insertCriteria: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        criteria: Criteria;
      }>
    ) => {
      const cohort = state.find((c) => c.id === action.payload.cohortId);
      if (cohort) {
        const group = cohort.groups.find(
          (g) => g.id === action.payload.groupId
        );
        if (group) {
          group.criteria.push(action.payload.criteria);
        }
      }
    },
    updateCriteriaData: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        criteriaId: string;
        data: unknown;
      }>
    ) => {
      const cohort = state.find((c) => c.id === action.payload.cohortId);
      if (cohort) {
        const group = cohort.groups.find(
          (g) => g.id === action.payload.groupId
        );
        if (group) {
          const criteria = group.criteria.find(
            (c) => c.id === action.payload.criteriaId
          );
          if (criteria) {
            criteria.data = action.payload.data;
          }
        }
      }
    },
    renameCriteria: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        criteriaId: string;
        criteriaName: string;
      }>
    ) => {
      const cohort = state.find((c) => c.id === action.payload.cohortId);
      if (cohort) {
        const group = cohort.groups.find(
          (g) => g.id === action.payload.groupId
        );
        if (group) {
          const criteria = group.criteria.find(
            (c) => c.id === action.payload.criteriaId
          );
          if (criteria) {
            criteria.name = action.payload.criteriaName;
          }
        }
      }
    },
    deleteCriteria: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        criteriaId: string;
      }>
    ) => {
      const cohort = state.find((c) => c.id === action.payload.cohortId);
      if (cohort) {
        const group = cohort.groups.find(
          (g) => g.id === action.payload.groupId
        );
        if (group) {
          group.criteria.splice(
            group.criteria.findIndex((c) => c.id === action.payload.criteriaId),
            1
          );
        }
      }
    },
  },
});

export const {
  insertCohort,
  insertGroup,
  renameGroup,
  insertCriteria,
  updateCriteriaData,
  renameCriteria,
  deleteCriteria,
} = cohortsSlice.actions;

export default cohortsSlice.reducer;
