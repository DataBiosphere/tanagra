SELECT po.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = po.procedure_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT mo.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.measurement` AS mo

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = mo.measurement_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT oo.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.observation` AS oo

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = oo.observation_source_concept_id

WHERE pc.type = 'CPT4'

UNION ALL

SELECT io.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.drug_exposure` AS io

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
