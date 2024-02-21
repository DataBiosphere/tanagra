SELECT
  vo.visit_occurrence_id,
  vo.person_id,
  p.person_source_value,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name,
  vo.visit_start_date,
  vo.visit_end_date,
  vo.visit_source_value,
  vo.visit_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(vo.visit_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence

FROM `${omopDataset}.visit_occurrence` AS vo

JOIN `${omopDataset}.person` AS p
    ON p.person_id = vo.person_id

JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
