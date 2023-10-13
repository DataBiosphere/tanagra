SELECT de.device_exposure_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.device_exposure` AS de

JOIN `all-of-us-ehr-dev.SR2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = de.device_concept_id

WHERE pc.type = 'CPT4'
