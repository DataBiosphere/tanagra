SELECT DISTINCT
  co.condition_occurrence_id,
  co.person_id,
  p.person_source_value,
  co.condition_concept_id,
  cc.concept_name AS condition_concept_name,
  cc.concept_code AS condition_concept_code,
  co.condition_start_date,
  co.condition_end_date,
  co.stop_reason,
  (CASE WHEN cc.standard_concept IS NULL THEN 'Source' WHEN cc.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
  co.condition_source_value,
  co.condition_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(co.condition_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  co.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.condition_occurrence` AS co

JOIN `${omopDataset}.person` AS p
    ON p.person_id = co.person_id

JOIN `${omopDataset}.concept` AS cc
    ON cc.concept_id = co.condition_concept_id

LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = co.visit_occurrence_id

LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
