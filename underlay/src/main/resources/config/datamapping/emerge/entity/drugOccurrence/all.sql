SELECT
  ROW_NUMBER() OVER() AS drug_occurrence_id,
  d.IND_SEQ as person_id,
  cast(parse_numeric(dc.criteria_meta_seq) as INT64) as drug_concept_id,
  dc.name AS drug_concept_name,
  CAST(FLOOR(PARSE_NUMERIC(d.age_at_event)) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.meds` AS d
JOIN `${omopDataset}.med_criteria` dc
ON lower(d.drug_name) = lower(dc.name)
