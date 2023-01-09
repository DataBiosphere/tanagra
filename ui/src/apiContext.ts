import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlaysV2(): Promise<tanagra.UnderlayListV2> {
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
  async listEntitiesV2(): Promise<tanagra.EntityListV2> {
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
  async queryInstances(): Promise<tanagra.InstanceListV2> {
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

class FakeStudiesAPI {}

class FakeCohortsAPI {}

class FakeConceptSetsAPI {}

class FakeReviewsAPI {}

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
