SELECT oo.observation_id, pc.id AS cpt4_id
FROM `${omopDataset}.observation` AS oo

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'
