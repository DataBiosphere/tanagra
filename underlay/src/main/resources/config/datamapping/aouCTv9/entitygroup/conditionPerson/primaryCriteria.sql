SELECT
  co.person_id,
  co.condition_concept_id
FROM `${omopDataset}.condition_occurrence` AS co
WHERE co.condition_concept_id IS NOT null
  AND co.condition_concept_id != 0
