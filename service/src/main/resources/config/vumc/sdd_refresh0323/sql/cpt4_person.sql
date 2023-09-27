SELECT po.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230328.procedure_occurrence` AS po

JOIN `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT mo.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230328.measurement` AS mo

JOIN `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT oo.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230328.observation` AS oo

JOIN `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT io.person_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230328.drug_exposure` AS io

JOIN `sd-vumc-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
