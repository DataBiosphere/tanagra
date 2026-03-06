SELECT id, name, code, subtype, concept_id, CAST(value AS INT64) AS value,
       survey_version_concept_id, survey_version_name
FROM `${staticTablesDataset}.prep_survey_enhanced`
WHERE survey_type = 'COPE'
  AND subtype IN ('SURVEY', 'QUESTION', 'ANSWER')
