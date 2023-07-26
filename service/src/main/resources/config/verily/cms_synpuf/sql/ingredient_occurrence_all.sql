SELECT
  de.drug_exposure_id AS id, de.person_id, de.drug_concept_id,
  de.drug_exposure_start_date AS start_date, de.drug_exposure_end_date AS end_date,
  de.stop_reason, de.refills, de.days_supply,
  de.drug_source_value AS source_value, de.drug_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(de.drug_exposure_start_date, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), DAY) / 365.25) AS INT64) AS age_at_occurrence,

FROM `bigquery-public-data.cms_synthetic_patient_data_omop.drug_exposure` AS de

JOIN `bigquery-public-data.cms_synthetic_patient_data_omop.person` AS p
ON p.person_id = de.person_id

