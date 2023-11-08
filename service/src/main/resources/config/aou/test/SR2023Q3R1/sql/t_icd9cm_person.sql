SELECT co.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.condition_occurrence` AS co

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
ON c.concept_id = co.condition_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'

UNION ALL

SELECT mo.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.measurement` AS mo

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
ON c.concept_id = mo.measurement_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'

UNION ALL

SELECT oo.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.observation` AS oo

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
ON c.concept_id = oo.observation_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'

UNION ALL

SELECT po.person_id, c.concept_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.procedure_occurrence` AS po

JOIN `all-of-us-ehr-dev.SR2023Q3R1.concept` AS c
ON c.concept_id = po.procedure_source_concept_id

WHERE c.vocabulary_id = 'ICD9CM'
