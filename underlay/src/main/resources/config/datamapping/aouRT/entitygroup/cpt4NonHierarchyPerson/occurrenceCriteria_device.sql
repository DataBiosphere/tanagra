SELECT de.device_exposure_id, c.concept_id AS cpt4_id
FROM `${omopDataset}.device_exposure` AS de
    JOIN `${omopDataset}.concept` AS c ON c.concept_id = de.device_source_concept_id
WHERE c.vocabulary_id = 'CPT4'
  AND c.concept_code NOT IN (
    SELECT pc.code FROM `${staticTablesDataset}.prep_cpt` pc WHERE pc.type='CPT4'
  )
