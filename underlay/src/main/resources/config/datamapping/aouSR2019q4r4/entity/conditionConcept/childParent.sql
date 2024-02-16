SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `${omopDataset}.concept` c2  ON c2.concept_id = cr.concept_id_2
JOIN `${omopDataset}.relationship` r ON cr.relationship_id = r.relationship_id
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Condition'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND c2.vocabulary_id = 'SNOMED'
  AND c1.standard_concept = c2.standard_concept
  AND c2.standard_concept = 'S'
  AND r.is_hierarchical = '1'
  AND r.defines_ancestry = '1'
