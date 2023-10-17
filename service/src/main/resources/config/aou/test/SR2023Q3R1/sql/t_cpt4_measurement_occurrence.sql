SELECT mo.measurement_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.measurement` AS mo

JOIN `all-of-us-ehr-dev.SR2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'
