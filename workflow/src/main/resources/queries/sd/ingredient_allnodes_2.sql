-- BuildPathsForHierarchy all-nodes input query for ingredients. This must match the entityMapping for the ingredient entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
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
