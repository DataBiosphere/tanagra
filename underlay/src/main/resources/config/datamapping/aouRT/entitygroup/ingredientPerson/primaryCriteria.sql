SELECT
  de.person_id,
  de.drug_concept_id
FROM `${omopDataset}.drug_exposure` AS de
WHERE de.drug_concept_id IS NOT null
  AND de.drug_concept_id != 0
