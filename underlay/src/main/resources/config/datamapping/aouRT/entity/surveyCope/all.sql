SELECT id, name, code, subtype, concept_id, CAST(value AS INT64) AS value
FROM `${omopDataset}.prep_survey`
WHERE survey IN ('May2020Covid19Participa', 'July2020Covid19Particip', 'February2021COVID19Part')
