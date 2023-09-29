SELECT
  vo.visit_occurrence_id AS id, vo.person_id, vo.visit_concept_id,
  vo.visit_start_date AS start_date, vo.visit_end_date AS end_date,
  vo.visit_source_value AS source_value, vo.visit_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(vo.visit_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence

FROM `sd-vumc-tanagra-test.sd_20230328.visit_occurrence` AS vo

JOIN `sd-vumc-tanagra-test.sd_20230328.person` AS p
ON p.person_id = vo.person_id
