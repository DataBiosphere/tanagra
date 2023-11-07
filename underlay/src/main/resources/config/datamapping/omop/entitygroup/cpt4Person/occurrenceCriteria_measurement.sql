SELECT mo.measurement_id, pc.id AS cpt4_id
FROM `${omopDataset}.measurement` AS mo

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'