import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { generateId } from "cohort";
import * as tanagra from "tanagra-api";

const initialState: tanagra.Cohort[] = [];

export const defaultFilter: tanagra.GroupFilter = {
  kind: tanagra.GroupFilterKindEnum.Any,
  excluded: false,
};

// TODO(tjennison): Normalize groups and criteria to simplify a lot of this
// nested code. This may require changing how the slices are arranged though,
// since having cohorts, groups, and criteria in separate slices may end up
// requiring too many special cases that cross slices.
const cohortsSlice = createSlice({
  name: "cohorts",
  initialState,
  reducers: {
    insertCohort: {
      reducer: (state, action: PayloadAction<tanagra.Cohort>) => {
        state.push(action.payload);
      },
      prepare: (name: string, underlayName: string) => ({
        payload: {
          id: generateId(),
          name,
          underlayName,
          groups: [
            {
              id: generateId(),
              filter: defaultFilter,
              criteria: [],
            },
          ],
        },
      }),
    },

    insertGroup: {
      reducer: (
        state,
        action: PayloadAction<{ cohortId: string; group: tanagra.Group }>
      ) => {
        const cohort = state.find((c) => c.id === action.payload.cohortId);
        if (cohort) {
          cohort.groups.push(action.payload.group);
        }
      },
      prepare: (cohortId: string, criteria?: tanagra.Criteria) => ({
        payload: {
          cohortId,
          group: {
            id: generateId(),
            filter: defaultFilter,
            criteria: criteria ? [criteria] : [],
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

    deleteGroup: {
      reducer: (
        state,
        action: PayloadAction<{
          cohortId: string;
          groupId: string;
          nextGroupId: string;
        }>
      ) => {
        const cohort = state.find((c) => c.id === action.payload.cohortId);
        if (cohort) {
          if (cohort.groups.length === 1) {
            // Clear the last group instead of deleting it so there's always at
            // least one group. Reusing the ID works more naturally for redo
            // because it sets the URL to where the the action was initiated
            // from, which would otherwise be the deleted group.
            cohort.groups = [
              {
                id: cohort.groups[0].id,
                filter: defaultFilter,
                criteria: [],
              },
            ];
          } else {
            cohort.groups = cohort.groups.filter(
              (group) => group.id !== action.payload.groupId
            );
          }
        }
      },
      prepare: (cohort: tanagra.Cohort, groupId: string) => {
        const groupIndex = cohort.groups.findIndex(
          (group) => group.id === groupId
        );
        if (groupIndex < 0) {
          throw new Error(
            `Group ${groupId} not found in cohort ${cohort.id} for deleteGroup.`
          );
        }

        let newIndex = groupIndex + 1;
        if (cohort.groups.length === 1) {
          newIndex = 0;
        } else if (groupIndex === cohort.groups.length - 1) {
          newIndex = groupIndex - 1;
        }

        return {
          payload: {
            cohortId: cohort.id,
            groupId,
            nextGroupId: cohort.groups[newIndex].id,
          },
        };
      },
    },

    setGroupFilter: {
      reducer: (
        state,
        action: PayloadAction<{
          cohortId: string;
          groupId: string;
          filter: tanagra.GroupFilter;
        }>
      ) => {
        const cohort = state.find((c) => c.id === action.payload.cohortId);
        if (cohort) {
          const group = cohort.groups.find(
            (g) => g.id === action.payload.groupId
          );
          if (group) {
            group.filter = action.payload.filter;
          }
        }
      },
      prepare: (
        cohortId: string,
        groupId: string,
        filter: tanagra.GroupFilter
      ) => ({
        payload: {
          cohortId,
          groupId,
          filter,
        },
      }),
    },

    insertCriteria: (
      state,
      action: PayloadAction<{
        cohortId: string;
        groupId: string;
        criteria: tanagra.Criteria;
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
        data: object;
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
  deleteGroup,
  setGroupFilter,
  insertCriteria,
  updateCriteriaData,
  deleteCriteria,
} = cohortsSlice.actions;

export default cohortsSlice.reducer;
