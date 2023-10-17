SELECT oo.observation_id, pc.id AS cpt4_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.observation` AS oo

JOIN `all-of-us-ehr-dev.SC2023Q3R1.prep_cpt` AS pc
ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'
