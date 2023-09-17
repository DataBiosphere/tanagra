SELECT oo.observation_id, pc.id AS cpt4_id
FROM `victr-tanagra-test.sd_20230328.observation` AS oo

JOIN `verily-tanagra-dev.aou_static_prep_useast1.prep_cpt` AS pc
ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'
