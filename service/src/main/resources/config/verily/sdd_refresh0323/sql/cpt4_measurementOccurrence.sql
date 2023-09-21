SELECT mo.measurement_id, pc.id AS cpt4_id
FROM `sd-vumc-tanagra-test.sd_20230331.measurement` AS mo

JOIN `verily-tanagra-dev.aou_static_prep_uscentral1.prep_cpt` AS pc
ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'