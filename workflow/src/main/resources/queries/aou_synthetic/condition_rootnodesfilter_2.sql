-- BuildPathsForHierarchy root-node-filter input query for conditions. Root nodes that are not included in this filter, will be removed from the hierarchy.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.concept_id = 441840
;
