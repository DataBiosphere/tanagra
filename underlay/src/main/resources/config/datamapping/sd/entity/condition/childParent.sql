SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `${omopDataset}.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Condition'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c2.vocabulary_id = 'SNOMED'
