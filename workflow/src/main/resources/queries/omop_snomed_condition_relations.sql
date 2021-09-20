-- FlattenHierarchy input query for OMOP SNOMED condition concept relationships.
-- TODO consider how we should template on the source dataset.
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `broad-tanagra-dev.synpuf.concept_relationship` cr
JOIN `broad-tanagra-dev.synpuf.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `broad-tanagra-dev.synpuf.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes' AND
  c1.domain_id = 'Condition' AND
  c2.domain_id = 'Condition' AND
  c1.vocabulary_id = 'SNOMED' AND
  c2.vocabulary_id = 'SNOMED'
;
