SELECT
  de.device_exposure_id,
  de.person_id,
  de.device_concept_id,
  dc.concept_name AS standard_concept_name,
  dc.concept_code AS standard_concept_code,
  dc.vocabulary_id AS standard_vocabulary,
  de.device_exposure_start_datetime,
  de.device_exposure_end_datetime,
  de.device_type_concept_id,
  dt.concept_name as device_type_concept_name,
  de.device_source_value,
  de.device_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.device_exposure_start_datetime, p.birth_datetime, DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id,
  vo.visit_concept_id,
  vc.concept_name AS visit_occurrence_concept_name,
  ds.concept_name AS source_concept_name,
  ds.concept_code AS source_concept_code,
  ds.vocabulary_id as source_vocabulary
FROM `${omopDataset}.device_exposure` AS de
JOIN `${omopDataset}.person` AS p ON p.person_id = de.person_id
JOIN `${omopDataset}.concept` AS dc ON dc.concept_id = de.device_concept_id
JOIN `${omopDataset}.concept` AS dt ON dt.concept_id = de.device_type_concept_id
JOIN `${omopDataset}.concept` AS ds ON ds.concept_id = de.device_source_concept_id
    AND dc.domain_id = 'Device'
    AND dc.standard_concept = 'S'
LEFT JOIN `${omopDataset}.visit_occurrence` AS vo ON vo.visit_occurrence_id = de.visit_occurrence_id
LEFT JOIN `${omopDataset}.concept` AS vc ON vc.concept_id = vo.visit_concept_id
