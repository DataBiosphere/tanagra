SELECT
  de.person_id,
  de.device_concept_id
FROM `${omopDataset}.device_exposure` AS de
WHERE de.device_concept_id IS NOT null
  AND de.device_concept_id !=0
