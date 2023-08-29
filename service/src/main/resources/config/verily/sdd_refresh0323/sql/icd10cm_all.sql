SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `victr-tanagra-test.sd_20230328.concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0

UNION ALL

SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `verily-tanagra-dev.aou_static_prep_useast1.prep_concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
