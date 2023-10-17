SELECT io.drug_exposure_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.drug_exposure` AS io

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
