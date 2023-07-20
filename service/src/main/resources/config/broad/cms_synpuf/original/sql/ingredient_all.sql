SELECT
  c.concept_id AS id, c.concept_name AS name, c.vocabulary_id, c.standard_concept, c.concept_code
FROM `bigquery-public-data.cms_synthetic_patient_data_omop.concept` c
WHERE c.domain_id = 'Drug'
AND (
    (c.vocabulary_id = 'ATC')
    OR (c.vocabulary_id IN ('RxNorm', 'RxNorm Extension'))
)
