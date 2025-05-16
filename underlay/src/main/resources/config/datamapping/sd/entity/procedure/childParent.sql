SELECT
    cr.concept_id_1 AS parent,
    cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.relationship` r ON cr.relationship_id = r.relationship_id
JOIN `${omopDataset}.concept` chld  ON cr.concept_id_2 = chld.concept_id
WHERE cr.relationship_id = 'Subsumes'
  AND r.is_hierarchical = '1'
  AND r.defines_ancestry = '1'
  AND chld.domain_id = 'Procedure'
  AND chld.vocabulary_id = 'SNOMED'
  AND chld.standard_concept = 'S'
