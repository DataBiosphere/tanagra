SELECT po.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.procedure_occurrence` AS po

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD9Proc'

UNION ALL

SELECT io.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.drug_exposure` AS io

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = io.drug_source_concept_id

WHERE c.vocabulary_id = 'ICD9Proc'
