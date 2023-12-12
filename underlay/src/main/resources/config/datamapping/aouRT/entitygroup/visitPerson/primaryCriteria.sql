SELECT
  vo.person_id,
  vo.visit_concept_id
FROM `${omopDataset}.visit_occurrence` AS vo
WHERE vo.visit_concept_id IS NOT null
    AND vo.visit_concept_id != 0
