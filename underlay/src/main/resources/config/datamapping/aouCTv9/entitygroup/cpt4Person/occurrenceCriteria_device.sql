SELECT de.device_exposure_id, pc.id AS cpt4_id
FROM `${omopDataset}.device_exposure` AS de
JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = de.device_source_concept_id
WHERE pc.type = 'CPT4'
  AND device_source_concept_id IS NOT null
  AND device_source_concept_id != 0
