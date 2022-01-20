-- FlattenHierarchy input query for OMOP concept relationships to generate the ancestor-descendant hierarchy table.
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `broad-tanagra-dev.synpuf.concept_relationship` cr
JOIN `broad-tanagra-dev.synpuf.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `broad-tanagra-dev.synpuf.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes' AND
  c1.domain_id = c2.domain_id
;
