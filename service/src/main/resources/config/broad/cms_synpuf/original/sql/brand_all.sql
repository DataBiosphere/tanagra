SELECT *
FROM `bigquery-public-data.cms_synthetic_patient_data_omop.concept`
WHERE
  domain_id = 'Drug' AND concept_class_id = 'Brand Name'
  AND vocabulary_id IN ('RxNorm', 'RxNorm Extension')
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0