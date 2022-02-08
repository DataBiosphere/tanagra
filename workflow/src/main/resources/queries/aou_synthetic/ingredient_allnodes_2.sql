-- BuildPathsForHierarchy all-nodes input query for ingredients. This must match the entityMapping for the ingredient entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Drug'
AND ((c.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
        AND c.concept_class_id = 'Ingredient'
        AND c.standard_concept = 'S')
    OR (c.vocabulary_id = 'RxNorm'
        AND c.concept_class_id = 'Precise Ingredient')
    OR (c.vocabulary_id = 'ATC'
        AND c.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
        AND c.standard_concept = 'C'))
AND c.valid_end_date > DATE('2022-01-01')
;
