SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `all-of-us-ehr-dev.SC2023Q3R1.concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0

UNION ALL

SELECT concept_id, concept_name, vocabulary_id, standard_concept, concept_code,
CASE WHEN concept_code IS NULL THEN concept_name ELSE CONCAT(concept_code, ' ', concept_name) END AS label
FROM `all-of-us-ehr-dev.SC2023Q3R1.prep_concept`
WHERE
  vocabulary_id = 'ICD10CM'
  AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
