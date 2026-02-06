SELECT id, name, code, subtype, concept_id, CAST(value AS INT64) AS value
FROM `${omopDataset}.prep_survey`
WHERE survey = 'SocialDeterminantsOfHea'
