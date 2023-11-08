SELECT co.person_id, c.concept_id
FROM `${omopDataset}.condition_occurrence` AS co

JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = co.condition_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'

UNION ALL

SELECT oo.person_id, c.concept_id
FROM `${omopDataset}.observation` AS oo

JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = oo.observation_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'

UNION ALL

SELECT po.person_id, c.concept_id
FROM `${omopDataset}.procedure_occurrence` AS po

JOIN `${omopDataset}.concept` AS c
    ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'
