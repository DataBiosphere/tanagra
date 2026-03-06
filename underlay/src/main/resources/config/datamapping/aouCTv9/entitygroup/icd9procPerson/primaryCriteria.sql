SELECT po.person_id, c.concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = po.procedure_source_concept_id
WHERE c.vocabulary_id = 'ICD9Proc'
  AND po.procedure_source_concept_id IS NOT null
  AND po.procedure_source_concept_id != 0

UNION ALL

SELECT io.person_id, c.concept_id
FROM `${omopDataset}.drug_exposure` AS io
JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = io.drug_source_concept_id
WHERE c.vocabulary_id = 'ICD9Proc'
  AND io.drug_source_concept_id IS NOT null
  AND io.drug_source_concept_id != 0
