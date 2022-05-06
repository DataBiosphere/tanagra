-- BuildPathsForHierarchy all-nodes input query for brands. This must match the entityMapping for the brand entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Drug'
AND c.concept_class_id = 'Brand Name'
AND c.invalid_reason IS NULL
AND c.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
;
