SELECT po.person_id, c.concept_id
FROM `victr-tanagra-test.sd_20230328.procedure_occurrence` AS po

JOIN `victr-tanagra-test.sd_20230328.concept` AS c
ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD10PCS'

UNION ALL

SELECT io.person_id, c.concept_id
FROM `victr-tanagra-test.sd_20230328.drug_exposure` AS io

JOIN `victr-tanagra-test.sd_20230328.concept` AS c
ON c.concept_id = io.drug_source_concept_id

WHERE c.vocabulary_id = 'ICD10PCS'
