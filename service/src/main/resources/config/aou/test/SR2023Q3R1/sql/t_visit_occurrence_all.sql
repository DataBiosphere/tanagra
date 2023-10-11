SELECT
  vo.visit_occurrence_id AS id, vo.person_id, vo.visit_concept_id,
  vo.visit_start_date AS start_date, vo.visit_end_date AS end_date,
  vo.visit_source_value AS source_value, vo.visit_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(vo.visit_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence

FROM `all-of-us-ehr-dev.SR2023Q3R1.visit_occurrence` AS vo

JOIN `all-of-us-ehr-dev.SR2023Q3R1.person` AS p
ON p.person_id = vo.person_id

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` c ON vo.visit_concept_id = c.concept_id
WHERE c.domain_id = 'Visit'
  AND c.standard_concept = 'S'
  AND vo.visit_concept_id > 0

