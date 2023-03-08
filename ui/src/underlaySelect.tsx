import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import ActionBar from "actionBar";
import { useAppSelector } from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { underlayURL } from "router";

export function UnderlaySelect() {
  const underlays = [
      {
          "name": "cms_synpuf",
          "displayName": "cms_synpuf",
          "primaryEntity": "person",
          "entities": [
              {
                  "name": "ingredient_occurrence",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "end_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      },
                      {
                          "name": "ingredient",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_criteria_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "stop_reason",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "visit_occurrence_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "days_supply",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_value",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "person_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "start_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      }
                  ]
              },
              {
                  "name": "condition",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "vocabulary",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "condition_occurrence",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "end_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      },
                      {
                          "name": "condition",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_criteria_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "stop_reason",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "visit_occurrence_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_value",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "person_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "start_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      }
                  ]
              },
              {
                  "name": "ingredient",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "vocabulary",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "device_occurrence",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "end_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      },
                      {
                          "name": "source_criteria_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "visit_occurrence_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "device",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_value",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "person_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "start_date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      }
                  ]
              },
              {
                  "name": "observation",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "vocabulary",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "person",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "gender",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "race",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "ethnicity",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "year_of_birth",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "observation_occurrence",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      },
                      {
                          "name": "unit",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_criteria_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "observation",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "value_as_string",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "visit_occurrence_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "value",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_value",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "person_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "procedure",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "vocabulary",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "procedure_occurrence",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "date",
                          "type": "SIMPLE",
                          "dataType": "DATE"
                      },
                      {
                          "name": "source_criteria_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "visit_occurrence_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      },
                      {
                          "name": "procedure",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "INT64"
                      },
                      {
                          "name": "source_value",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "person_id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "brand",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              },
              {
                  "name": "device",
                  "idAttribute": "id",
                  "attributes": [
                      {
                          "name": "standard_concept",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "vocabulary",
                          "type": "KEY_AND_DISPLAY",
                          "dataType": "STRING"
                      },
                      {
                          "name": "name",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "concept_code",
                          "type": "SIMPLE",
                          "dataType": "STRING"
                      },
                      {
                          "name": "id",
                          "type": "SIMPLE",
                          "dataType": "INT64"
                      }
                  ]
              }
          ],
          "uiConfiguration": {
              "dataConfig": {
                  "primaryEntity": {
                      "displayName": "Person",
                      "entity": "person",
                      "key": "id"
                  },
                  "occurrences": [
                      {
                          "id": "condition_occurrence",
                          "displayName": "Condition Occurrences",
                          "entity": "condition_occurrence",
                          "key": "id",
                          "classifications": [
                              {
                                  "id": "condition",
                                  "attribute": "condition",
                                  "entity": "condition",
                                  "entityAttribute": "id",
                                  "hierarchy": "standard",
                                  "defaultSort": {
                                      "attribute": "t_rollup_count",
                                      "direction": "DESC"
                                  }
                              }
                          ]
                      },
                      {
                          "id": "procedure_occurrence",
                          "displayName": "Procedure Occurrences",
                          "entity": "procedure_occurrence",
                          "key": "id",
                          "classifications": [
                              {
                                  "id": "procedure",
                                  "attribute": "procedure",
                                  "entity": "procedure",
                                  "entityAttribute": "id",
                                  "hierarchy": "standard",
                                  "defaultSort": {
                                      "attribute": "t_rollup_count",
                                      "direction": "DESC"
                                  }
                              }
                          ]
                      },
                      {
                          "id": "observation_occurrence",
                          "displayName": "Observation Occurrence",
                          "entity": "observation_occurrence",
                          "key": "id",
                          "classifications": [
                              {
                                  "id": "observation",
                                  "attribute": "observation",
                                  "entity": "observation",
                                  "entityAttribute": "id",
                                  "defaultSort": {
                                      "attribute": "t_rollup_count",
                                      "direction": "DESC"
                                  }
                              }
                          ]
                      },
                      {
                          "id": "ingredient_occurrence",
                          "displayName": "Drug Occurrence",
                          "entity": "ingredient_occurrence",
                          "key": "drug",
                          "classifications": [
                              {
                                  "id": "ingredient",
                                  "attribute": "drug",
                                  "entity": "ingredient",
                                  "entityAttribute": "id",
                                  "hierarchy": "standard",
                                  "defaultSort": {
                                      "attribute": "t_rollup_count",
                                      "direction": "DESC"
                                  },
                                  "groupings": [
                                      {
                                          "id": "brand",
                                          "entity": "brand",
                                          "defaultSort": {
                                              "attribute": "name",
                                              "direction": "ASC"
                                          },
                                          "attributes": [
                                              "name",
                                              "id",
                                              "standard_concept",
                                              "concept_code"
                                          ]
                                      }
                                  ]
                              }
                          ]
                      }
                  ]
              },
              "criteriaConfigs": [
                  {
                      "type": "classification",
                      "id": "tanagra-conditions",
                      "title": "Condition",
                      "conceptSet": true,
                      "category": "Domains",
                      "columns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Concept name"
                          },
                          {
                              "key": "id",
                              "width": 100,
                              "title": "Concept ID"
                          },
                          {
                              "key": "standard_concept",
                              "width": 120,
                              "title": "Source/standard"
                          },
                          {
                              "key": "vocabulary_t_value",
                              "width": 120,
                              "title": "Vocab"
                          },
                          {
                              "key": "concept_code",
                              "width": 120,
                              "title": "Code"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "hierarchyColumns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Condition"
                          },
                          {
                              "key": "id",
                              "width": 120,
                              "title": "Concept ID"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "occurrence": "condition_occurrence",
                      "classification": "condition"
                  },
                  {
                      "type": "classification",
                      "id": "tanagra-procedures",
                      "title": "Procedure",
                      "conceptSet": true,
                      "category": "Domains",
                      "columns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Concept name"
                          },
                          {
                              "key": "id",
                              "width": 100,
                              "title": "Concept ID"
                          },
                          {
                              "key": "standard_concept",
                              "width": 120,
                              "title": "Source/standard"
                          },
                          {
                              "key": "vocabulary_t_value",
                              "width": 120,
                              "title": "Vocab"
                          },
                          {
                              "key": "concept_code",
                              "width": 120,
                              "title": "Code"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "hierarchyColumns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Procedure"
                          },
                          {
                              "key": "id",
                              "width": 120,
                              "title": "Concept ID"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "occurrence": "procedure_occurrence",
                      "classification": "procedure"
                  },
                  {
                      "type": "classification",
                      "id": "tanagra-observations",
                      "title": "Observation",
                      "conceptSet": true,
                      "category": "Domains",
                      "columns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Concept name"
                          },
                          {
                              "key": "id",
                              "width": 100,
                              "title": "Concept ID"
                          },
                          {
                              "key": "standard_concept",
                              "width": 120,
                              "title": "Source/standard"
                          },
                          {
                              "key": "vocabulary_t_value",
                              "width": 120,
                              "title": "Vocab"
                          },
                          {
                              "key": "concept_code",
                              "width": 120,
                              "title": "Code"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "occurrence": "observation_occurrence",
                      "classification": "observation"
                  },
                  {
                      "type": "classification",
                      "id": "tanagra-drugs",
                      "title": "Drug",
                      "conceptSet": true,
                      "category": "Domains",
                      "columns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Concept name"
                          },
                          {
                              "key": "id",
                              "width": 100,
                              "title": "Concept ID"
                          },
                          {
                              "key": "standard_concept",
                              "width": 120,
                              "title": "Source/standard"
                          },
                          {
                              "key": "vocabulary_t_value",
                              "width": 120,
                              "title": "Vocab"
                          },
                          {
                              "key": "concept_code",
                              "width": 120,
                              "title": "Code"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "hierarchyColumns": [
                          {
                              "key": "name",
                              "width": "100%",
                              "title": "Drug"
                          },
                          {
                              "key": "id",
                              "width": 120,
                              "title": "Concept ID"
                          },
                          {
                              "key": "t_rollup_count",
                              "width": 120,
                              "title": "Roll-up count"
                          }
                      ],
                      "occurrence": "ingredient_occurrence",
                      "classification": "ingredient"
                  },
                  {
                      "type": "attribute",
                      "id": "tanagra-ethnicity",
                      "title": "Ethnicity",
                      "category": "Program data",
                      "attribute": "ethnicity"
                  },
                  {
                      "type": "attribute",
                      "id": "tanagra-gender",
                      "title": "Gender identity",
                      "category": "Program data",
                      "attribute": "gender"
                  },
                  {
                      "type": "attribute",
                      "id": "tanagra-race",
                      "title": "Race",
                      "category": "Program data",
                      "attribute": "race"
                  },
                  {
                      "type": "attribute",
                      "id": "tanagra-sex_at_birth",
                      "title": "Sex assigned at birth",
                      "category": "Program data",
                      "attribute": "sex_at_birth"
                  },
                  {
                      "type": "attribute",
                      "id": "tanagra-year_of_birth",
                      "title": "Year of birth",
                      "category": "Program data",
                      "attribute": "year_of_birth"
                  }
              ],
              "demographicChartConfigs": {
                  "groupByAttributes": [
                      "gender",
                      "race",
                      "year_of_birth"
                  ],
                  "chartConfigs": [
                      {
                          "title": "Gender identity",
                          "primaryProperties": [
                              {
                                  "key": "gender"
                              }
                          ]
                      },
                      {
                          "title": "Gender identity, Current age, Race",
                          "primaryProperties": [
                              {
                                  "key": "gender"
                              },
                              {
                                  "key": "age",
                                  "buckets": [
                                      {
                                          "min": 18,
                                          "max": 45,
                                          "displayName": "18-44"
                                      },
                                      {
                                          "min": 45,
                                          "max": 65,
                                          "displayName": "45-64"
                                      },
                                      {
                                          "min": 65,
                                          "displayName": "65+"
                                      }
                                  ]
                              }
                          ],
                          "stackedProperty": {
                              "key": "race"
                          }
                      }
                  ]
              },
              "prepackagedConceptSets": [
                  {
                      "id": "_demographics",
                      "name": "Demographics",
                      "occurrence": ""
                  },
                  {
                      "id": "_analgesics",
                      "name": "Analgesics",
                      "occurrence": "ingredient_occurrence",
                      "filter": {
                          "type": "CLASSIFICATION",
                          "occurrenceID": "ingredient_occurrence",
                          "classificationID": "ingredient",
                          "keys": [
                              21604253
                          ]
                      }
                  }
              ],
              "criteriaSearchConfig": {
                  "criteriaTypeWidth": 120,
                  "columns": [
                      {
                          "key": "name",
                          "width": "100%",
                          "title": "Concept Name"
                      },
                      {
                          "key": "vocabulary_t_value",
                          "width": 120,
                          "title": "Vocab"
                      },
                      {
                          "key": "concept_code",
                          "width": 120,
                          "title": "Code"
                      },
                      {
                          "key": "t_rollup_count",
                          "width": 120,
                          "title": "Roll-up Count"
                      }
                  ]
              }
          }
      }
  ];
  return (
    <>
      <ActionBar title="Select Dataset" backURL={null} />
      <List>
        {underlays.map((underlay) => (
          <ListItem key={underlay.name}>
            <ListItemButton
              component={RouterLink}
              to={underlayURL(underlay.name)}
            >
              <ListItemText primary={underlay.name}></ListItemText>
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </>
  );
}
