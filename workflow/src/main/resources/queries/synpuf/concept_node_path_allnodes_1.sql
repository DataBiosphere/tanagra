-- BuildPathsForHierarchy all-nodes input query for OMOP concept relationships to generate the node-path hierarchy table.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.synpuf.concept` c
;
