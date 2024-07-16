SELECT
  ROW_NUMBER() OVER() AS lab_occurrence_id,
  l.IND_SEQ as person_id,
  cast(parse_numeric(lc.criteria_meta_seq) as INT64) as lab_concept_id,
  l.lab_name AS lab_concept_name,
  l.result AS value_as_number,
  l.age_at_event AS age_at_occurrence
FROM `${omopDataset}.labs` AS l
JOIN `${omopDataset}.lab_criteria` lc
ON l.lab_name = lc.name
