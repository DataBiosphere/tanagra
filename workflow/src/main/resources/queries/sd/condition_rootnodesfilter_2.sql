-- BuildPathsForHierarchy root-node-filter input query for conditions. Root nodes that are not included in this filter, will be removed from the hierarchy.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.concept_id = 441840
;
