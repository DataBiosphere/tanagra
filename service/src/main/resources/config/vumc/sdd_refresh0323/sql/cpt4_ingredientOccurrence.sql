SELECT io.drug_exposure_id, pc.id AS cpt4_id
FROM `victr-tanagra-test.sd_20230328.drug_exposure` AS io

JOIN `victr-tanagra-test.aou_static_prep.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
