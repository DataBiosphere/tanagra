-- BuildPathsForHierarchy all-nodes input query for brands. This must match the entityMapping for the brand entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Drug'
AND c.concept_class_id = 'Brand Name'
AND c.invalid_reason IS NULL
AND c.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
;
