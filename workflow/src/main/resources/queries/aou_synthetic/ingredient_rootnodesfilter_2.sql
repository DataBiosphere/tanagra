-- BuildPathsForHierarchy root-node-filter input query for ingredients. Root nodes that are not included in this filter, will be removed from the hierarchy.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.vocabulary_id = 'ATC'
AND c.concept_class_id = 'ATC 1st'
AND c.standard_concept = 'C'
AND c.valid_end_date > DATE('2022-01-01')
;
