SELECT po.person_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.procedure_occurrence` AS po

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT mo.person_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.measurement` AS mo

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT oo.person_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.observation` AS oo

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT io.person_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.drug_exposure` AS io

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
