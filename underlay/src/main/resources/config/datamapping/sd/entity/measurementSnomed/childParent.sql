SELECT
    cr.concept_id_1 AS parent,
    cr.concept_id_2 AS child,
FROM `${omopDataset}.concept_relationship` cr
JOIN `${omopDataset}.concept` chld  ON cr.concept_id_2 = chld.concept_id
JOIN `${omopDataset}.concept` parnt  ON cr.concept_id_1 = parnt.concept_id
WHERE cr.relationship_id = 'Subsumes'
  AND parnt.vocabulary_id = chld.vocabulary_id
  AND parnt.vocabulary_id = 'SNOMED'
  AND parnt.domain_id = chld.domain_id
  AND parnt.domain_id = 'Measurement'
  AND parnt.standard_concept = 'S'
