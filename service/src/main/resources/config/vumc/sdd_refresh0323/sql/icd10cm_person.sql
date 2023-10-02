SELECT co.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.condition_occurrence` AS co

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = co.condition_source_concept_id

WHERE c.vocabulary_id = 'ICD10CM'

UNION ALL

SELECT mo.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = mo.measurement_source_concept_id

WHERE c.vocabulary_id = 'ICD10CM'

UNION ALL

SELECT oo.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.observation` AS oo

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = oo.observation_source_concept_id

WHERE c.vocabulary_id = 'ICD10CM'

UNION ALL

SELECT po.person_id, c.concept_id
FROM `sd-vumc-tanagra-test.sd_20230331.procedure_occurrence` AS po

JOIN `sd-vumc-tanagra-test.sd_20230331.concept` AS c
ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD10CM'
