-- BuildPathsForHierarchy parent-nodes input query for OMOP concept relationships to generate the node-path hierarchy table.
-- Must include the named parameter @node, so we can lookup the parent of an arbitrary node.
SELECT
  cr.concept_id_1 AS parent
FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept_relationship` cr
WHERE
  cr.relationship_id = 'Subsumes' AND
  cr.concept_id_2 = @node
;
