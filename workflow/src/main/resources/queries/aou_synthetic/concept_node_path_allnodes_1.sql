-- BuildPathsForHierarchy all-nodes input query for OMOP concept relationships to generate the node-path hierarchy table.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE
  c.concept_id IN (201826, 201820, 4024659)
;
