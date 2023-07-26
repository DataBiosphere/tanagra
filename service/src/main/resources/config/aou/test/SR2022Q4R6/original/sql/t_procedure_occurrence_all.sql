SELECT
  po.procedure_occurrence_id AS id, po.person_id, po.procedure_concept_id, po.procedure_date,
  po.procedure_source_value AS source_value, po.procedure_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  po.visit_occurrence_id, vo.visit_concept_id

FROM `all-of-us-ehr-dev.SR2022Q4R6.procedure_occurrence` AS po

JOIN `all-of-us-ehr-dev.SR2022Q4R6.person` AS p
ON p.person_id = po.person_id

LEFT JOIN `all-of-us-ehr-dev.SR2022Q4R6.visit_occurrence` AS vo
ON vo.visit_occurrence_id = po.visit_occurrence_id
