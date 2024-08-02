SELECT
  co.condition_occurrence_id,
  co.person_id,
  co.condition_concept_id,
  cc.concept_name AS standard_concept_name,
  cc.concept_code AS standard_concept_code,
  cc.vocabulary_id AS standard_vocabulary,
  co.condition_start_datetime,
  co.condition_end_datetime,
  co.stop_reason,
  co.condition_source_value,
  co.condition_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(co.condition_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  co.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name as visit_occurrence_concept_name,
  co.condition_type_concept_id,
  ct.concept_name as condition_type_concept_name,
  sc.concept_name AS source_concept_name,
  sc.concept_code AS source_concept_code,
  sc.vocabulary_id AS source_vocabulary,
  co.condition_status_source_value,
  co.condition_status_concept_id,
  cs.concept_name as condition_status_concept_name
FROM `${omopDataset}.condition_occurrence` AS co
JOIN `${omopDataset}.person` AS p ON p.person_id = co.person_id
JOIN `${omopDataset}.concept` AS cc ON cc.concept_id = co.condition_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = co.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
LEFT JOIN `${omopDataset}.concept` AS sc ON sc.concept_id = co.condition_source_concept_id
LEFT JOIN `${omopDataset}.concept` AS ct ON ct.concept_id = co.condition_type_concept_id
LEFT JOIN `${omopDataset}.concept` AS cs ON cs.concept_id = co.condition_status_concept_id
WHERE co.condition_concept_id IS NOT null
  AND co.condition_concept_id != 0
