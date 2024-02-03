SELECT de.person_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.device_exposure` AS de
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = de.device_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )

UNION ALL

SELECT io.person_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.drug_exposure` AS io
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = io.drug_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )

UNION ALL

SELECT mo.person_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.measurement` AS mo
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = mo.measurement_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )

UNION ALL

SELECT oo.person_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.observation` AS oo
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = oo.observation_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )

UNION ALL

SELECT po.person_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.procedure_occurrence` AS po
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = po.procedure_source_value
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )
