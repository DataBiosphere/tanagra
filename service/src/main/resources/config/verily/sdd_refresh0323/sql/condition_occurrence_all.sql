SELECT
  co.condition_occurrence_id AS id, co.person_id, co.condition_concept_id,
  co.condition_start_date AS start_date, co.condition_end_date AS end_date, co.stop_reason,
  co.condition_source_value AS source_value, co.condition_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(co.condition_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  co.visit_occurrence_id, vo.visit_concept_id

FROM `victr-tanagra-test.sd_20230328.condition_occurrence` AS co

JOIN `victr-tanagra-test.sd_20230328.person` AS p
ON p.person_id = co.person_id

LEFT JOIN `victr-tanagra-test.sd_20230328.visit_occurrence` AS vo
ON vo.visit_occurrence_id = co.visit_occurrence_id
