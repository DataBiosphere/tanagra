SELECT
    concept_id,
    concept_name,
    vocabulary_id,
    concept_code,
    (CASE WHEN standard_concept IS NULL THEN 'Source' WHEN standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS standard_concept
FROM `${omopDataset}.concept`
WHERE
  domain_id = 'Drug' AND concept_class_id = 'Brand Name'
  AND vocabulary_id IN ('RxNorm', 'RxNorm Extension')
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0