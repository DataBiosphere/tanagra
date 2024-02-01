SELECT co.person_id, c.concept_id
FROM `${omopDataset}.condition_occurrence` AS co
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = co.condition_source_concept_id
WHERE c.vocabulary_id = 'ICD9CM'
  AND c.concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
)

UNION ALL

SELECT mo.person_id, c.concept_id
FROM `${omopDataset}.measurement` AS mo
         JOIN `${omopDataset}.concept` AS c
              ON c.concept_id = mo.measurement_source_concept_id
WHERE c.vocabulary_id = 'ICD9CM'
  AND c.concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
)

UNION ALL

SELECT oo.person_id, c.concept_id
FROM `${omopDataset}.observation` AS oo
         JOIN `${omopDataset}.concept` AS c
              ON c.concept_id = oo.observation_source_concept_id
WHERE c.vocabulary_id = 'ICD9CM'
  AND c.concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
)

UNION ALL

SELECT po.person_id, c.concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = po.procedure_source_concept_id
WHERE c.vocabulary_id = 'ICD9CM'
  AND c.concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9CM'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
)
