import Box from "@mui/material/Box";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import Loading from "components/loading";
import { useAppDispatch } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import { useCallback } from "react";
import { RouterProvider } from "react-router-dom";
import { createAppRouter } from "router";
import { fetchUserData } from "storage/storage";
import useSWRImmutable from "swr/immutable";
import { setUnderlays, Underlay } from "underlaysSlice";
import "./app.css";
import { SortDirection } from "./data/configuration";
import { FilterType } from "./data/filter";
import { AttributeV2TypeEnum, DataTypeV2, EntityListV2 } from "./tanagra-api";
import theme from "./theme";

enableMapSet();

export default function App() {
  const dispatch = useAppDispatch();

  const underlaysState = useSWRImmutable(
    { component: "App" },
    useCallback(async () => {
      const res: { underlays: Underlay[] } = {
        underlays: [
          {
            name: "cms_synpuf",
            displayName: "cms_synpuf",
            primaryEntity: "person",
            uiConfiguration: {
              dataConfig: {
                primaryEntity: {
                  entity: "person",
                  key: "id",
                },
                occurrences: [
                  {
                    id: "condition_occurrence",
                    entity: "condition_occurrence",
                    key: "id",
                    classifications: [
                      {
                        id: "condition",
                        attribute: "condition",
                        entity: "condition",
                        entityAttribute: "id",
                        hierarchy: "standard",
                        defaultSort: {
                          attribute: "t_rollup_count",
                          direction: SortDirection.Desc,
                        },
                      },
                    ],
                  },
                  {
                    id: "procedure_occurrence",
                    entity: "procedure_occurrence",
                    key: "id",
                    classifications: [
                      {
                        id: "procedure",
                        attribute: "procedure",
                        entity: "procedure",
                        entityAttribute: "id",
                        hierarchy: "standard",
                        defaultSort: {
                          attribute: "t_rollup_count",
                          direction: SortDirection.Desc,
                        },
                      },
                    ],
                  },
                  {
                    id: "observation_occurrence",
                    entity: "observation_occurrence",
                    key: "id",
                    classifications: [
                      {
                        id: "observation",
                        attribute: "observation",
                        entity: "observation",
                        entityAttribute: "id",
                        defaultSort: {
                          attribute: "t_rollup_count",
                          direction: SortDirection.Desc,
                        },
                      },
                    ],
                  },
                  {
                    id: "ingredient_occurrence",
                    entity: "ingredient_occurrence",
                    key: "drug",
                    classifications: [
                      {
                        id: "ingredient",
                        attribute: "drug",
                        entity: "ingredient",
                        entityAttribute: "id",
                        hierarchy: "standard",
                        defaultSort: {
                          attribute: "t_rollup_count",
                          direction: SortDirection.Desc,
                        },
                        groupings: [
                          {
                            id: "brand",
                            entity: "brand",
                            defaultSort: {
                              attribute: "name",
                              direction: SortDirection.Asc,
                            },
                            attributes: [
                              "name",
                              "id",
                              "standard_concept",
                              "concept_code",
                            ],
                          },
                        ],
                      },
                    ],
                  },
                ],
              },
              criteriaConfigs: [
                {
                  type: "classification",
                  id: "tanagra-conditions",
                  title: "Condition",
                  conceptSet: true,
                  category: "Domains",
                },
                {
                  type: "classification",
                  id: "tanagra-procedures",
                  title: "Procedure",
                  conceptSet: true,
                  category: "Domains",
                },
                {
                  type: "classification",
                  id: "tanagra-observations",
                  title: "Observation",
                  conceptSet: true,
                  category: "Domains",
                },
                {
                  type: "classification",
                  id: "tanagra-drugs",
                  title: "Drug",
                  conceptSet: true,
                  category: "Domains",
                },
                {
                  type: "attribute",
                  id: "tanagra-ethnicity",
                  title: "Ethnicity",
                  category: "Program data",
                },
                {
                  type: "attribute",
                  id: "tanagra-gender",
                  title: "Gender identity",
                  category: "Program data",
                },
                {
                  type: "attribute",
                  id: "tanagra-race",
                  title: "Race",
                  category: "Program data",
                },
                {
                  type: "attribute",
                  id: "tanagra-sex_at_birth",
                  title: "Sex assigned at birth",
                  category: "Program data",
                },
                {
                  type: "attribute",
                  id: "tanagra-year_of_birth",
                  title: "Year of birth",
                  category: "Program data",
                },
              ],
              demographicChartConfigs: {
                groupByAttributes: ["gender", "race", "year_of_birth"],
                chartConfigs: [
                  {
                    title: "Gender identity",
                    primaryProperties: [{ key: "gender" }],
                  },
                  {
                    title: "Gender identity, Current age, Race",
                    primaryProperties: [
                      { key: "gender" },
                      {
                        key: "age",
                        buckets: [
                          { min: 18, max: 45, displayName: "18-44" },
                          { min: 45, max: 65, displayName: "45-64" },
                          { min: 65, displayName: "65+" },
                        ],
                      },
                    ],
                    stackedProperty: { key: "race" },
                  },
                ],
              },
              prepackagedConceptSets: [
                {
                  id: "_demographics",
                  name: "Demographics",
                  occurrence: "",
                },
                {
                  id: "_analgesics",
                  name: "Analgesics",
                  occurrence: "ingredient_occurrence",
                  filter: {
                    type: FilterType.Classification,
                    occurrenceID: "ingredient_occurrence",
                    classificationID: "ingredient",
                    keys: [21604253],
                  },
                },
              ],
              criteriaSearchConfig: {
                criteriaTypeWidth: "120",
                columns: [
                  {
                    key: "name",
                    width: "100%",
                    title: "Concept Name",
                  },
                  {
                    key: "vocabulary_t_value",
                    width: 120,
                    title: "Vocab",
                  },
                  {
                    key: "concept_code",
                    width: 120,
                    title: "Code",
                  },
                  { key: "t_rollup_count", width: 120, title: "Roll-up Count" },
                ],
              },
            },
          },
        ],
      };
      if (!res?.underlays || res.underlays.length == 0) {
        throw new Error("No underlays are configured.");
      }

      const underlays = await Promise.all(
        res.underlays.map(async (underlay) => {
          const entitiesRes: EntityListV2 = {
            entities: [
              {
                name: "ingredient_occurrence",
                idAttribute: "id",
                attributes: [
                  {
                    name: "end_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                  {
                    name: "ingredient",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_criteria_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "stop_reason",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "visit_occurrence_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "days_supply",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_value",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "person_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "start_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                ],
              },
              {
                name: "condition",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "vocabulary",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "condition_occurrence",
                idAttribute: "id",
                attributes: [
                  {
                    name: "end_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                  {
                    name: "condition",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_criteria_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "stop_reason",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "visit_occurrence_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_value",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "person_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "start_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                ],
              },
              {
                name: "ingredient",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "vocabulary",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "device_occurrence",
                idAttribute: "id",
                attributes: [
                  {
                    name: "end_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                  {
                    name: "source_criteria_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "visit_occurrence_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "device",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_value",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "person_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "start_date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                ],
              },
              {
                name: "observation",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "vocabulary",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "person",
                idAttribute: "id",
                attributes: [
                  {
                    name: "gender",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "race",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "ethnicity",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "year_of_birth",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "observation_occurrence",
                idAttribute: "id",
                attributes: [
                  {
                    name: "date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                  {
                    name: "unit",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_criteria_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "observation",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "value_as_string",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "visit_occurrence_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "value",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_value",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "person_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "procedure",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "vocabulary",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "procedure_occurrence",
                idAttribute: "id",
                attributes: [
                  {
                    name: "date",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Date,
                  },
                  {
                    name: "source_criteria_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "visit_occurrence_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "procedure",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.Int64,
                  },
                  {
                    name: "source_value",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "person_id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "brand",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
              {
                name: "device",
                idAttribute: "id",
                attributes: [
                  {
                    name: "standard_concept",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "vocabulary",
                    type: AttributeV2TypeEnum.KeyAndDisplay,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "name",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "concept_code",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.String,
                  },
                  {
                    name: "id",
                    type: AttributeV2TypeEnum.Simple,
                    dataType: DataTypeV2.Int64,
                  },
                ],
              },
            ],
          };
          if (!entitiesRes?.entities) {
            throw new Error(`No entities in underlay ${underlay.name}`);
          }

          if (!underlay.uiConfiguration) {
            throw new Error(`No UI configuration in underlay ${name}`);
          }

          return {
            name: underlay.name,
            displayName: underlay.displayName ?? underlay.name,
            primaryEntity: underlay.primaryEntity,
            entities: entitiesRes.entities,
            uiConfiguration: underlay.uiConfiguration,
          };
        })
      );

      await fetchUserData(dispatch, underlays);

      dispatch(setUnderlays(underlays));
      return underlays;
    }, [])
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Loading status={underlaysState}>
        <Box
          sx={{
            display: "grid",
            width: "100%",
            height: "100%",
            gridTemplateColumns: "1fr",
            gridTemplateRows: (theme) => `${theme.spacing(6)} 1fr`,
            gridTemplateAreas: "'actionBar' 'content'",
          }}
        >
          <Box
            sx={{
              gridArea: "content",
              width: "100%",
              minWidth: "100%",
              height: "100%",
              minHeight: "100%",
            }}
          >
            <RouterProvider router={createAppRouter()} />
          </Box>
        </Box>
      </Loading>
    </ThemeProvider>
  );
}
