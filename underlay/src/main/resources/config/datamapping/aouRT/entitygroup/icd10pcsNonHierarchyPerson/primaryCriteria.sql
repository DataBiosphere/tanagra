SELECT po.person_id, c.concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = po.procedure_source_concept_id
WHERE c.vocabulary_id = 'ICD10PCS'
  AND c.concept_id NOT IN (
  SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD10PCS'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
  UNION ALL
  SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD10PCS'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
  )

UNION ALL

SELECT io.person_id, c.concept_id
FROM `${omopDataset}.drug_exposure` AS io
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = io.drug_source_concept_id
WHERE c.vocabulary_id = 'ICD10PCS'
  AND c.concept_id NOT IN (
    SELECT concept_id FROM `${omopDataset}.concept`
    WHERE vocabulary_id = 'ICD10PCS'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
    UNION ALL
    SELECT concept_id FROM `${staticTablesDataset}.prep_concept`
    WHERE vocabulary_id = 'ICD10PCS'
      AND DATE_DIFF(CAST(valid_end_date AS DATE), CURRENT_DATE(), DAY) > 0
  )
