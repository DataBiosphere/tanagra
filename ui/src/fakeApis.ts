import * as tanagra from "tanagra-api";

// TODO(tjennison): Figure out a more comprehensive solutions for faking APIs.
export class FakeUnderlaysApi {
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
          featureSet: true,
          columns,
          occurrence: "condition_occurrence",
          classification: "condition",
        },
        {
          type: "classification",
          id: "tanagra-observation",
          title: "Observation",
          featureSet: true,
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

export class FakeStudiesAPI {
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

export class FakeCohortsAPI {
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

export class FakeFeatureSetsAPI {
  async listFeatureSets(): Promise<Array<tanagra.FeatureSet>> {
    return [
      {
        id: "test_feature_set",
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

export class FakeReviewsAPI {
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

export class FakeAnnotationsAPI {}

export class FakeExportAPI {}

export class FakeUsersAPI {}
