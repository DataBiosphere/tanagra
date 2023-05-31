import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlays(): Promise<tanagra.UnderlayListV2> {
    const columns = [{ key: "name", width: "100%", title: "Concept name" }];

    const uiConfiguration = {
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
                attribute: "condition_concept_id",
                entity: "condition",
                entityAttribute: "id",
                hierarchy: "standard",
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
                attribute: "observation_concept_id",
                entity: "observation",
                entityAttribute: "id",
              },
            ],
          },
        ],
      },
      criteriaConfigs: [
        {
          type: "classification",
          id: "tanagra-condition",
          title: "Condition",
          conceptSet: true,
          columns,
          occurrence: "condition_occurrence",
          classification: "condition",
        },
        {
          type: "classification",
          id: "tanagra-observation",
          title: "Observation",
          conceptSet: true,
          columns,
          occurrence: "observation_occurrence",
          classification: "observation",
        },
        {
          type: "attribute",
          id: "tanagra-race",
          title: "Race",
          attribute: "race",
        },
        {
          type: "attribute",
          id: "tanagra-year_of_birth",
          title: "Year of birth",
          attribute: "year_of_birth",
        },
      ],
      criteriaSearchConfig: {
        criteriaTypeWidth: 120,
        columns: [
          { key: "name", width: "100%", title: "Concept name" },
          { key: "t_rollup_count", width: 120, title: "Roll-up count" },
        ],
      },
    };

    return {
      underlays: [
        {
          name: "underlay_name",
          displayName: "Test Underlay",
          primaryEntity: "person",
          uiConfiguration: JSON.stringify(uiConfiguration),
        },
      ],
    };
  }
}

class FakeEntitiesApi {
  async listEntities(): Promise<tanagra.EntityListV2> {
    return {
      entities: [
        {
          name: "person",
          idAttribute: "id",
          attributes: [
            {
              name: "id",
              dataType: tanagra.DataTypeV2.Int64,
            },
            {
              name: "race",
              dataType: tanagra.DataTypeV2.Int64,
            },
            {
              name: "year_of_birth",
              dataType: tanagra.DataTypeV2.Int64,
            },
          ],
        },
        {
          name: "condition_occurrence",
          idAttribute: "concept_id",
          attributes: [
            {
              name: "id",
              dataType: tanagra.DataTypeV2.Int64,
            },
            {
              name: "name",
              dataType: tanagra.DataTypeV2.String,
            },
          ],
        },
      ],
    };
  }
}

class FakeInstancesApi {
  async listInstances(): Promise<tanagra.InstanceListResultV2> {
    return {
      sql: "SELECT * FROM table WHERE xyz;",
      instances: [
        {
          relationshipFields: [
            {
              count: 42,
            },
          ],
          attributes: {
            id: {
              value: {
                dataType: tanagra.DataTypeV2.Int64,
                valueUnion: {
                  int64Val: 1234,
                },
              },
            },
            name: {
              value: {
                dataType: tanagra.DataTypeV2.String,
                valueUnion: {
                  stringVal: "test concept",
                },
              },
            },
          },
        },
      ],
    };
  }
}

class FakeHintsApi {
  async queryHints(): Promise<tanagra.DisplayHintListV2> {
    return {
      displayHints: [
        {
          attribute: {
            name: "race",
          },
          displayHint: {
            enumHint: {
              enumHintValues: [
                {
                  enumVal: {
                    display: "Asian",
                    value: {
                      dataType: tanagra.DataTypeV2.Int64,
                      valueUnion: {
                        int64Val: 8515,
                      },
                    },
                  },
                  count: 123,
                },
                {
                  enumVal: {
                    display: "Black or African American",
                    value: {
                      dataType: tanagra.DataTypeV2.Int64,
                      valueUnion: {
                        int64Val: 8516,
                      },
                    },
                  },
                  count: 456,
                },
              ],
            },
          },
        },
        {
          attribute: {
            name: "year_of_birth",
          },
          displayHint: {
            numericRangeHint: {
              min: 21,
              max: 79,
            },
          },
        },
      ],
    };
  }
}

class FakeStudiesAPI {
  async listStudies(): Promise<Array<tanagra.StudyV2>> {
    return [
      {
        id: "test_study",
        created: new Date(),
        createdBy: "test_user",
        displayName: "Test Study",
        lastModified: new Date(),
      },
    ];
  }
}

class FakeCohortsAPI {
  async listCohorts(): Promise<Array<tanagra.CohortV2>> {
    return [
      {
        id: "test_cohort",
        created: new Date(),
        createdBy: "test_user",
        displayName: "Test Cohort",
        lastModified: new Date(),
        underlayName: "test_underlay",
        criteriaGroupSections: [],
      },
    ];
  }
}

class FakeConceptSetsAPI {
  async listConceptSets(): Promise<Array<tanagra.ConceptSetV2>> {
    return [
      {
        id: "test_concept_set",
        created: new Date(),
        createdBy: "test_user",
        displayName: "Test data feature",
        lastModified: new Date(),
        underlayName: "test_underlay",
        entity: "test_entity",
        criteria: {
          id: "entity_id",
          displayName: "test_entity",
          pluginName: "test_plugin",
          selectionData: "test_data",
          uiConfig: "test_config",
          tags: {},
        },
      },
    ];
  }
}

class FakeReviewsAPI {
  async listReviews(): Promise<Array<tanagra.ReviewV2>> {
    return [
      {
        id: "test_review",
        created: new Date(),
        createdBy: "test_user",
        displayName: "Test Review",
        lastModified: new Date(),
        size: 0,
      },
    ];
  }
}

class FakeAnnotationsAPI {}

function apiForEnvironment<Real, Fake>(
  real: { new (c: tanagra.Configuration): Real },
  fake: { new (): Fake }
) {
  const fn = () => {
    if (process.env.REACT_APP_USE_FAKE_API === "y") {
      return new fake();
    }

    const config: tanagra.ConfigurationParameters = {
      basePath: process.env.REACT_APP_BACKEND_HOST || "",
    };
    return new real(new tanagra.Configuration(config));
  };
  return React.createContext(fn());
}

export const UnderlaysApiContext = apiForEnvironment(
  tanagra.UnderlaysV2Api,
  FakeUnderlaysApi
);
export const EntityInstancesApiContext = apiForEnvironment(
  tanagra.InstancesV2Api,
  FakeInstancesApi
);
export const EntitiesApiContext = apiForEnvironment(
  tanagra.EntitiesV2Api,
  FakeEntitiesApi
);
export const HintsApiContext = apiForEnvironment(
  tanagra.HintsV2Api,
  FakeHintsApi
);
export const StudiesApiContext = apiForEnvironment(
  tanagra.StudiesV2Api,
  FakeStudiesAPI
);
export const CohortsApiContext = apiForEnvironment(
  tanagra.CohortsV2Api,
  FakeCohortsAPI
);
export const ConceptSetsApiContext = apiForEnvironment(
  tanagra.ConceptSetsV2Api,
  FakeConceptSetsAPI
);
export const ReviewsApiContext = apiForEnvironment(
  tanagra.ReviewsV2Api,
  FakeReviewsAPI
);
export const AnnotationsApiContext = apiForEnvironment(
  tanagra.AnnotationsV2Api,
  FakeAnnotationsAPI
);
