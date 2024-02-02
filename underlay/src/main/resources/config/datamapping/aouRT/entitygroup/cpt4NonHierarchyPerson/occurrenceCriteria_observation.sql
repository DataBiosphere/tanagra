SELECT oo.observation_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.observation` AS oo
    JOIN `${omopDataset}.concept` AS c ON c.concept_code = oo.observation_source_value
WHERE vocabulary_id = 'CPT4'
  AND concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )
