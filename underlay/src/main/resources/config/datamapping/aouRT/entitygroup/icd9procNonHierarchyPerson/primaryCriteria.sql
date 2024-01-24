SELECT co.person_id, c.concept_id
FROM `${omopDataset}.procedure_occurrence` AS co
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = co.procedure_source_concept_id
WHERE c.vocabulary_id = 'ICD9Proc'
  AND c.concept_id NOT IN(
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD9Proc'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
)
