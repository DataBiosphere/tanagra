SELECT po.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.procedure_occurrence` AS po

JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` AS c
ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD10PCS'

UNION ALL

SELECT io.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.drug_exposure` AS io

JOIN `all-of-us-ehr-dev.SC2023Q3R1.concept` AS c
ON c.concept_id = io.drug_source_concept_id

WHERE c.vocabulary_id = 'ICD10PCS'
