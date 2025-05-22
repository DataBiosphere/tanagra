SELECT
  co.person_id,
  co.condition_concept_id
FROM `${omopDataset}.condition_occurrence` AS co
WHERE co.condition_concept_id
  IN (SELECT c.concept_id FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Condition'
        AND c.vocabulary_id != 'SNOMED'
        AND c.standard_concept = 'S'
     )
AND co.condition_concept_id IS NOT NULL
AND co.condition_concept_id != 0
