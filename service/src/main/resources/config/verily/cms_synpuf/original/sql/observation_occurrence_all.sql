SELECT
  o.observation_id AS id, o.person_id, o.observation_concept_id,
  o.observation_date, o.value_as_string, o.value_as_concept_id, o.unit_concept_id,
  o.observation_source_value AS source_value, o.observation_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(o.observation_date, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), DAY) / 365.25) AS INT64) AS age_at_occurrence,

FROM `bigquery-public-data.cms_synthetic_patient_data_omop.observation` AS o

JOIN `bigquery-public-data.cms_synthetic_patient_data_omop.person` AS p
ON p.person_id = o.person_id

