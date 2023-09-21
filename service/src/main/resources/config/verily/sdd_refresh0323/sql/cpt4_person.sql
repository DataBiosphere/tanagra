SELECT po.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.procedure_occurrence` AS po

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT mo.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT oo.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.observation` AS oo

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT io.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.drug_exposure` AS io

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
