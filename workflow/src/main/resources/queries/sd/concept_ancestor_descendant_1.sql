-- FlattenHierarchy input query for OMOP concept relationships to generate the ancestor-descendant hierarchy table.
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `victr-tanagra-test.sd_static.concept_relationship` cr
JOIN `victr-tanagra-test.sd_static.concept` cparent  ON cparent.concept_id = cr.concept_id_1
JOIN `victr-tanagra-test.sd_static.concept` cchild  ON cchild.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND cparent.domain_id = cchild.domain_id
;
