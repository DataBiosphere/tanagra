SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `sd-vumc-tanagra-test.sd_20230331.concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0

UNION ALL

SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `sd-vumc-tanagra-test.aou_static_prep.prep_concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
