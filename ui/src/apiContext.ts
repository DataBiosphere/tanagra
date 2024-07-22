import { getAuth0Token, isAuth0Enabled, isAuthenticated } from "auth/auth0OAuth";
import { getEnvironment } from "environment";
import React from "react";
import * as tanagra from "tanagra-api";

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
        underlay: "{}",
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

export function getAccessToken(): () => Promise<string> {
  // If Auth0 is enabled: fetch it from Auth0
  // For local dev: get the bearer token from the iframe url param.
  // For all other envs: check parent window's localStorage for auth token.
  const env = getEnvironment();

  if (isAuth0Enabled() && isAuthenticated) {
    return getAuth0Token();
  } else {
    const token =
      (env.REACT_APP_GET_LOCAL_AUTH_TOKEN
        ? new URLSearchParams(window.location.href.split("?")[1]).get("token")
        : window.parent.localStorage.getItem("tanagraAccessToken")) ?? "";

    return () =>
      new Promise<string>((resolve) => {
        setTimeout(() => {
          resolve(token);
        });
      });
  }
}

function apiForEnvironment<Real, Fake>(
  real: { new (c: tanagra.Configuration): Real },
  fake: { new (): Fake },
  getAccessToken: () => Promise<string>
) {
  const env = getEnvironment();
  const fn = () => {
    if (env.REACT_APP_USE_FAKE_API === "y") {
      return new fake();
    }

    const config: tanagra.ConfigurationParameters = {
      basePath: env.REACT_APP_BACKEND_HOST || "",
      accessToken: getAccessToken,
    };
    return new real(new tanagra.Configuration(config));
  };
  return React.createContext(fn());
}

export function getUnderlaysApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(
    tanagra.UnderlaysApi,
    FakeUnderlaysApi,
    getAccessToken
  );
}

export function getStudiesApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(tanagra.StudiesApi, FakeStudiesAPI, getAccessToken);
}

export function getCohortsApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(tanagra.CohortsApi, FakeCohortsAPI, getAccessToken);
}

export function getConceptSetsApiContext(
  getAccessToken: () => Promise<string>
) {
  return apiForEnvironment(
    tanagra.ConceptSetsApi,
    FakeConceptSetsAPI,
    getAccessToken
  );
}

export function getReviewsApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(tanagra.ReviewsApi, FakeReviewsAPI, getAccessToken);
}

export function getAnnotationsApiContext(
  getAccessToken: () => Promise<string>
) {
  return apiForEnvironment(
    tanagra.AnnotationsApi,
    FakeAnnotationsAPI,
    getAccessToken
  );
}

export function getExportApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(tanagra.ExportApi, FakeExportAPI, getAccessToken);
}

export function getUsersApiContext(getAccessToken: () => Promise<string>) {
  return apiForEnvironment(tanagra.UsersApi, FakeUsersAPI, getAccessToken);
}
