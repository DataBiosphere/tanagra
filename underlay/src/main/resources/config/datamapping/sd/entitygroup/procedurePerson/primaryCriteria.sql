SELECT
  po.person_id,
  po.procedure_concept_id
FROM `${omopDataset}.procedure_occurrence` AS po
WHERE po.procedure_concept_id IS NOT null
  AND po.procedure_concept_id != 0
