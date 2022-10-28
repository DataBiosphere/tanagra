import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlays(): Promise<tanagra.ListUnderlaysResponse> {
    const columns = [
      { key: "concept_name", width: "100%", title: "Concept name" },
    ];

    const uiConfiguration = {
      dataConfig: {
        primaryEntity: {
          entity: "person",
          key: "person_id",
        },
        occurrences: [
          {
            id: "condition_occurrence",
            entity: "condition_occurrence",
            key: "concept_id",
            classifications: [
              {
                id: "condition",
                attribute: "condition_concept_id",
                entity: "condition",
                entityAttribute: "concept_id",
                hierarchical: true,
              },
            ],
          },
          {
            id: "observation_occurrence",
            entity: "observation_occurrence",
            key: "concept_id",
            classifications: [
              {
                id: "observation",
                attribute: "observation_concept_id",
                entity: "observation",
                entityAttribute: "concept_id",
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
          attribute: "race_concept_id",
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
          { key: "concept_name", width: "100%", title: "Concept name" },
          { key: "person_count", width: 120, title: "Roll-up count" },
        ],
      },
    };

    return new Promise<tanagra.ListUnderlaysResponse>((resolve) => {
      resolve({
        underlays: [
          {
            name: "underlay_name",
            entityNames: ["person"],
            uiConfiguration: JSON.stringify(uiConfiguration),
          },
        ],
      });
    });
  }
}

class FakeEntitiesApi {
  async listEntities(): Promise<tanagra.ListEntitiesResponse> {
    return new Promise<tanagra.ListEntitiesResponse>((resolve) => {
      resolve({
        entities: [
          {
            name: "person",
            attributes: [
              {
                name: "attribute_name",
                dataType: tanagra.DataType.Int64,
                attributeFilterHint: {
                  integerBoundsHint: {
                    min: 1,
                    max: 10,
                  },
                },
              },
              {
                name: "race_concept_id",
                dataType: tanagra.DataType.Int64,
                attributeFilterHint: {
                  enumHint: {
                    enumHintValues: [
                      {
                        displayName: "Asian",
                        description: "",
                        attributeValue: {
                          int64Val: 8515,
                        },
                      },
                      {
                        displayName: "Black or African American",
                        description: "",
                        attributeValue: {
                          int64Val: 8516,
                        },
                      },
                    ],
                  },
                },
              },
              {
                name: "year_of_birth",
                dataType: tanagra.DataType.Int64,
                attributeFilterHint: {
                  integerBoundsHint: {
                    min: 21,
                    max: 79,
                  },
                },
              },
            ],
          },
          {
            name: "condition_occurrence",
            attributes: [
              {
                name: "attribute_name",
                dataType: tanagra.DataType.Int64,
                attributeFilterHint: {
                  enumHint: {
                    enumHintValues: [
                      {
                        displayName: "Yes",
                        description: "Yes description",
                        attributeValue: {
                          int64Val: 2001,
                        },
                      },
                      {
                        displayName: "No",
                        description: "No description",
                        attributeValue: {
                          int64Val: 2002,
                        },
                      },
                    ],
                  },
                },
              },
            ],
          },
        ],
      });
    });
  }
}

class FakeEntityInstancesApi {
  async searchEntityInstances(): Promise<tanagra.SearchEntityInstancesResponse> {
    return new Promise<tanagra.SearchEntityInstancesResponse>((resolve) => {
      resolve({
        instances: [
          {
            concept_name: {
              stringVal: "test concept",
            },
            concept_id: {
              int64Val: 1234,
            },
          },
        ],
      });
    });
  }

  async generateDatasetSqlQuery(): Promise<tanagra.SqlQuery> {
    return new Promise<tanagra.SqlQuery>((resolve) => {
      resolve({ query: "SELECT * FROM table WHERE xyz;" });
    });
  }
}

class FakeEntityCountsApi {
  async searchEntityCounts(): Promise<tanagra.SearchEntityCountsResponse> {
    return new Promise<tanagra.SearchEntityCountsResponse>((resolve) => {
      resolve({
        counts: [
          {
            count: 52,
            definition: {
              gender_concept_id: {
                int64Val: 45880669,
              },
              gender: {
                stringVal: "Female",
              },
              race: {
                stringVal: "Black or African American",
              },
              race_concept_id: {
                int64Val: 8516,
              },
              year_of_birth: {
                int64Val: 2000,
              },
            },
          },
        ],
      });
    });
  }
}

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
  tanagra.UnderlaysApi,
  FakeUnderlaysApi
);
export const EntityInstancesApiContext = apiForEnvironment(
  tanagra.EntityInstancesApi,
  FakeEntityInstancesApi
);
export const EntitiesApiContext = apiForEnvironment(
  tanagra.EntitiesApi,
  FakeEntitiesApi
);
export const EntityCountsApiContext = apiForEnvironment(
  tanagra.EntityCountsApi,
  FakeEntityCountsApi
);
