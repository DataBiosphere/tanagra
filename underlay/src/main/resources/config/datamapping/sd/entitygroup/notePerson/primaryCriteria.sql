SELECT
  n.person_id,
  n.note_type_concept_id
FROM `${omopDataset}.note` AS n
WHERE n.note_type_concept_id IS NOT null
  AND n.note_type_concept_id != 0
