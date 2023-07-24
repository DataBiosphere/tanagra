SELECT
  co.condition_occurrence_id AS id, co.person_id, co.condition_concept_id,
  co.condition_start_date AS start_date, co.condition_end_date AS end_date, co.stop_reason,
  co.condition_source_value AS source_value, co.condition_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(co.condition_start_date, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), DAY) / 365.25) AS INT64) AS age_at_occurrence,

FROM `bigquery-public-data.cms_synthetic_patient_data_omop.condition_occurrence` AS co

JOIN `bigquery-public-data.cms_synthetic_patient_data_omop.person` AS p
ON p.person_id = co.person_id

