import React from "react";
import * as tanagra from "./tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
class FakeUnderlaysApi {
  async listUnderlaySummaries(): Promise<tanagra.UnderlaySummaryList> {
    return {
      underlays: [
        {
          name: "underlay_name",
          displayName: "Test Underlay",
          primaryEntity: "person",
        },
      ],
    };
  }

  async getUnderlay(req: { underlayName: string }): Promise<tanagra.Underlay> {
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
      summary: {
        name: req.underlayName,
        displayName: "Test Underlay",
        primaryEntity: "person",
      },
      serializedConfiguration: {
        underlay: "",
        entities: [],
        groupItemsEntityGroups: [],
        criteriaOccurrenceEntityGroups: [],
      },
      uiConfiguration: JSON.stringify(uiConfiguration),
    };
  }

  async listEntities(): Promise<tanagra.EntityList> {
    return {
      entities: [
        {
          name: "person",
          idAttribute: "id",
          attributes: [
            {
              name: "id",
              dataType: tanagra.DataType.Int64,
            },
            {
              name: "race",
              dataType: tanagra.DataType.Int64,
            },
            {
              name: "year_of_birth",
              dataType: tanagra.DataType.Int64,
            },
          ],
        },
        {
          name: "condition_occurrence",
          idAttribute: "concept_id",
          attributes: [
            {
              name: "id",
              dataType: tanagra.DataType.Int64,
            },
            {
              name: "name",
              dataType: tanagra.DataType.String,
            },
          ],
        },
      ],
    };
  }

  async listInstances(): Promise<tanagra.InstanceListResult> {
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
                dataType: tanagra.DataType.Int64,
                valueUnion: {
                  int64Val: 1234,
                },
              },
            },
            name: {
              value: {
                dataType: tanagra.DataType.String,
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

  async queryHints(): Promise<tanagra.DisplayHintList> {
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
                      dataType: tanagra.DataType.Int64,
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
                      dataType: tanagra.DataType.Int64,
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
  async listStudies(): Promise<Array<tanagra.Study>> {
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
  async listCohorts(): Promise<Array<tanagra.Cohort>> {
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
  async listConceptSets(): Promise<Array<tanagra.ConceptSet>> {
    return [
      {
        id: "test_concept_set",
        created: new Date(),
        createdBy: "test_user",
        displayName: "Test data feature",
        lastModified: new Date(),
        underlayName: "test_underlay",
        criteria: [
          {
            id: "entity_id",
            displayName: "test_entity",
            pluginName: "test_plugin",
            selectionData: "test_data",
            uiConfig: "test_config",
            tags: {},
          },
        ],
        entityOutputs: [
          {
            entity: "test_entity",
            excludeAttributes: ["test_attribute"],
          },
        ],
      },
    ];
  }
}

class FakeReviewsAPI {
  async listReviews(): Promise<Array<tanagra.Review>> {
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

class FakeExportAPI {}

class FakeUsersAPI {}

const getAccessToken = () => {
  let accessToken = "";
  if (process.env.REACT_APP_GET_LOCAL_AUTH_TOKEN) {
    // For local dev only, get the bearer token from the iframe url param
    accessToken =
      new URLSearchParams(window.location.href.split("?")[1]).get("token") ??
      "";
  } else {
    // Check parent window's localStorage for auth token
    // Use try/catch block to prevent UI from breaking in case of CORS error
    try {
      accessToken =
        window.parent.localStorage.getItem("tanagraAccessToken") ?? "";
    } catch (e) {
      console.error(e);
    }
  }
  return accessToken;
};

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
      accessToken: getAccessToken(),
    };
    return new real(new tanagra.Configuration(config));
  };
  return React.createContext(fn());
}

export const UnderlaysApiContext = apiForEnvironment(
  tanagra.UnderlaysApi,
  FakeUnderlaysApi
);
export const StudiesApiContext = apiForEnvironment(
  tanagra.StudiesApi,
  FakeStudiesAPI
);
export const CohortsApiContext = apiForEnvironment(
  tanagra.CohortsApi,
  FakeCohortsAPI
);
export const ConceptSetsApiContext = apiForEnvironment(
  tanagra.ConceptSetsApi,
  FakeConceptSetsAPI
);
export const ReviewsApiContext = apiForEnvironment(
  tanagra.ReviewsApi,
  FakeReviewsAPI
);
export const AnnotationsApiContext = apiForEnvironment(
  tanagra.AnnotationsApi,
  FakeAnnotationsAPI
);
export const ExportApiContext = apiForEnvironment(
  tanagra.ExportApi,
  FakeExportAPI
);
export const UsersApiContext = apiForEnvironment(
  tanagra.UsersApi,
  FakeUsersAPI
);
