SELECT
  co.condition_occurrence_id,
  co.person_id,
  co.condition_concept_id,
  cc.concept_name AS condition_concept_name,
  co.condition_start_date,
  co.condition_end_date,
  co.stop_reason,
  co.condition_source_value,
  co.condition_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(co.condition_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  co.visit_occurrence_id

FROM `${omopDataset}.condition_occurrence` AS co

JOIN `${omopDataset}.person` AS p
    ON p.person_id = co.person_id

JOIN `${omopDataset}.concept` AS cc
    ON cc.concept_id = co.condition_concept_id
