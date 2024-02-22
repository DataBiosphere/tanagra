SELECT
  po.procedure_occurrence_id,
  po.person_id,
  p.person_source_value,
  po.procedure_concept_id,
  pc.concept_name AS procedure_concept_name,
  po.procedure_date,
  po.procedure_source_value,
  po.procedure_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_date, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  po.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.procedure_occurrence` AS po

JOIN `${omopDataset}.person` AS p
    ON p.person_id = po.person_id

JOIN `${omopDataset}.concept` AS pc
    ON pc.concept_id = po.procedure_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = po.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
