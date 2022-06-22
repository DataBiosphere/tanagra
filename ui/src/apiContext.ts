import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlays(): Promise<tanagra.ListUnderlaysResponse> {
    const columns = [
      { key: "concept_name", width: "100%", title: "Concept Name" },
    ];

    const criteriaConfigs = [
      {
        type: "concept",
        title: "Conditions",
        defaultName: "Contains Conditions Codes",
        plugin: {
          columns,
          entities: [
            { name: "condition", selectable: true, hierarchical: true },
          ],
        },
      },
      {
        type: "concept",
        title: "Observations",
        defaultName: "Contains Observations Codes",
        plugin: {
          columns,
          entities: [{ name: "observation", selectable: true }],
        },
      },
      {
        type: "attribute",
        title: "Race",
        defaultName: "Contains Race Codes",
        plugin: {
          attribute: "race_concept_id",
        },
      },
      {
        type: "attribute",
        title: "Year at Birth",
        defaultName: "Contains Year at Birth Values",
        plugin: {
          attribute: "year_of_birth",
        },
      },
    ];

    return new Promise<tanagra.ListUnderlaysResponse>((resolve) => {
      resolve({
        underlays: [
          {
            name: "underlay_name",
            entityNames: ["person"],
            criteriaConfigs: JSON.stringify(criteriaConfigs),
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
