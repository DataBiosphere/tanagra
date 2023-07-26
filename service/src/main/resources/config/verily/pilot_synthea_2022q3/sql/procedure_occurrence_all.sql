SELECT
  po.procedure_occurrence_id AS id, po.person_id, po.procedure_concept_id, po.procedure_date,
  po.procedure_source_value AS source_value, po.procedure_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  po.visit_occurrence_id, vo.visit_concept_id

FROM `verily-tanagra-dev.pilot_synthea_2022q3.procedure_occurrence` AS po

JOIN `verily-tanagra-dev.pilot_synthea_2022q3.person` AS p
ON p.person_id = po.person_id

LEFT JOIN `verily-tanagra-dev.pilot_synthea_2022q3.visit_occurrence` AS vo
ON vo.visit_occurrence_id = po.visit_occurrence_id
