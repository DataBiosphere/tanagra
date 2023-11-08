SELECT
  de.device_exposure_id,
  de.person_id,
  de.device_concept_id,
  dc.concept_name AS device_concept_name,
  de.device_exposure_start_date,
  de.device_exposure_end_date,
  de.device_source_value,
  de.device_source_concept_id,
  CAST(FLOOR(TIMESTAMP_DIFF(TIMESTAMP(de.device_exposure_start_date), TIMESTAMP(CONCAT(p.year_of_birth,'-',p.month_of_birth,'-',p.day_of_birth)), DAY) / 365.25) AS INT64) AS age_at_occurrence,
  de.visit_occurrence_id

FROM `${omopDataset}.device_exposure` AS de

JOIN `${omopDataset}.person` AS p
    ON p.person_id = de.person_id

JOIN `${omopDataset}.concept` AS dc
    ON dc.concept_id = de.device_concept_id
