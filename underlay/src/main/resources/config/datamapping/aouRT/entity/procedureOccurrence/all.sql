SELECT
  po.procedure_occurrence_id,
  po.person_id,
  po.procedure_concept_id,
  pc.concept_name AS standard_concept_name,
  pc.concept_code AS standard_concept_code,
  pc.vocabulary_id AS standard_vocabulary,
  po.procedure_date,
  po.procedure_datetime,
  po.procedure_type_concept_id,
  pt.concept_name as procedure_type_concept_name,
  po.modifier_concept_id,
  po.modifier_source_value,
  pm.concept_name as modifier_concept_name,
  po.quantity,
  po.procedure_source_value,
  po.procedure_source_concept_id,
  ps.concept_name as source_concept_name,
  ps.concept_code as source_concept_code,
  ps.vocabulary_id as source_vocabulary,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  po.visit_occurrence_id,
  vc.concept_name as visit_occurrence_concept_name,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name
FROM `${omopDataset}.procedure_occurrence` AS po
JOIN `${omopDataset}.person` AS p ON p.person_id = po.person_id
JOIN `${omopDataset}.concept` AS pc ON pc.concept_id = po.procedure_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = po.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS pt ON pt.concept_id = po.procedure_type_concept_id
LEFT JOIN `${omopDataset}.concept` AS pm ON pm.concept_id = po.modifier_concept_id
LEFT JOIN `${omopDataset}.concept` AS ps ON ps.concept_id = po.procedure_source_concept_id
WHERE po.procedure_concept_id IS NOT null
  AND po.procedure_concept_id != 0
