SELECT
  po.procedure_occurrence_id AS id, po.person_id, po.procedure_concept_id, po.procedure_dat,
  po.procedure_source_value AS source_value, po.procedure_source_concept_id AS source_criteria_id,
  CAST(FLOOR(TIMESTAMP_DIFF(po.procedure_dat, DATETIME(p.year_of_birth, p.month_of_birth, p.day_of_birth, 0, 0, 0), DAY) / 365.25) AS INT64) AS age_at_occurrence,

FROM `bigquery-public-data.cms_synthetic_patient_data_omop.procedure_occurrence` AS po

JOIN `bigquery-public-data.cms_synthetic_patient_data_omop.person` AS p
ON p.person_id = po.person_id

