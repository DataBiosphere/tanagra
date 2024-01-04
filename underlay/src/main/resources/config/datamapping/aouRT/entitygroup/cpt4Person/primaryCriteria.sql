SELECT io.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.device_exposure` AS io
    JOIN `${staticTablesDataset}.prep_cpt` AS pc ON pc.concept_id = io.device_source_concept_id
WHERE pc.type = 'CPT4'
  AND io.device_source_concept_id IS NOT null
  AND io.device_source_concept_id != 0

UNION ALL

SELECT io.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.drug_exposure` AS io
    JOIN `${staticTablesDataset}.prep_cpt` AS pc ON pc.concept_id = io.drug_source_concept_id
WHERE pc.type = 'CPT4'
  AND io.drug_source_concept_id IS NOT null
  AND io.drug_source_concept_id != 0

UNION ALL

SELECT mo.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.measurement` AS mo
    JOIN `${staticTablesDataset}.prep_cpt` AS pc ON pc.concept_id = mo.measurement_source_concept_id
WHERE pc.type = 'CPT4'
  AND mo.measurement_source_concept_id IS NOT null
  AND mo.measurement_source_concept_id != 0

UNION ALL

SELECT oo.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.observation` AS oo
    JOIN `${staticTablesDataset}.prep_cpt` AS pc ON pc.concept_id = oo.observation_source_concept_id
WHERE pc.type = 'CPT4'
  AND oo.observation_source_concept_id IS NOT null
  AND oo.observation_source_concept_id != 0

UNION ALL

SELECT po.person_id, pc.id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po
    JOIN `${staticTablesDataset}.prep_cpt` AS pc ON pc.concept_id = po.procedure_source_concept_id
WHERE pc.type = 'CPT4'
  AND po.procedure_source_concept_id IS NOT null
  AND po.procedure_source_concept_id != 0
