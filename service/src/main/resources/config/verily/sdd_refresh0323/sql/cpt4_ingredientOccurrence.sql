SELECT io.drug_exposure_id, pc.id AS cpt4_id
FROM `victr-tanagra-test.sd_20230328.drug_exposure` AS io

JOIN `verily-tanagra-dev.aou_static_prep_useast1.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
