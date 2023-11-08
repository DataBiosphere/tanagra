SELECT
  po.procedure_occurrence_id,
  po.person_id,
  po.procedure_concept_id,
  pc.concept_name AS procedure_concept_name,
  po.procedure_dat,
  po.procedure_source_value,
  po.procedure_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_dat, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  po.visit_occurrence_id

FROM `${omopDataset}.procedure_occurrence` AS po

JOIN `${omopDataset}.person` AS p
    ON p.person_id = po.person_id

JOIN `${omopDataset}.concept` AS pc
    ON pc.concept_id = po.procedure_concept_id
