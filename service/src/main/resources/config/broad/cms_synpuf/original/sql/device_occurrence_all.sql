SELECT
  de.device_exposure_id AS id, de.person_id, de.device_concept_id,
  de.device_exposure_start_date AS start_date, de.device_exposure_end_date AS end_date,
  de.device_source_value AS source_value, de.device_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.device_exposure_start_date, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), DAY) / 365.25) AS INT64) AS age_at_occurrence,

FROM `bigquery-public-data.cms_synthetic_patient_data_omop.device_exposure` AS de

JOIN `bigquery-public-data.cms_synthetic_patient_data_omop.person` AS p
ON p.person_id = de.person_id

