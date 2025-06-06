SELECT
  de.device_exposure_id,
  de.person_id,
  p.person_source_value,
  de.device_concept_id,
  dc.concept_name AS device_concept_name,
  de.device_exposure_start_date,
  de.device_exposure_end_date,
  de.device_source_value,
  de.device_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.device_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_concept_name

FROM `${omopDataset}.device_exposure` AS de
JOIN `${omopDataset}.person` AS p
    ON p.person_id = de.person_id
JOIN `${omopDataset}.concept` AS dc
    ON dc.concept_id = de.device_concept_id
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo
    ON vo.visit_occurrence_id = de.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc
    ON vc.concept_id = vo.visit_concept_id
WHERE de.device_concept_id IS NOT null
  AND de.device_concept_id != 0
