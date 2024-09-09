SELECT
  vo.visit_occurrence_id,
  vo.person_id,
  vo.visit_concept_id,
  vc.concept_name AS standard_concept_name,
  vo.visit_start_datetime,
  vo.visit_end_datetime,
  vo.visit_source_value,
  vo.visit_source_concept_id,
  vsc.concept_name AS source_concept_name,
  CAST(FLOOR(TIMESTAMP_DIFF(vo.visit_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence
FROM `${omopDataset}.visit_occurrence` AS vo
JOIN `${omopDataset}.person` AS p ON p.person_id = vo.person_id
JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
    AND vc.domain_id = 'Visit'
    AND vc.standard_concept = 'S'
JOIN `${omopDataset}.concept` AS vsc ON vsc.concept_id = vo.visit_source_concept_id
WHERE vo.visit_concept_id IS NOT null
  AND vo.visit_concept_id != 0
