SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `victr-tanagra-test.sd_static.concept_relationship` cr
JOIN `victr-tanagra-test.sd_static.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `victr-tanagra-test.sd_static.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id IN ('Has form', 'RxNorm - ATC', 'RxNorm - ATC name', 'Mapped from', 'Subsumes')
  AND c1.concept_id != c2.concept_id

  AND c1.domain_id = 'Drug'
  AND ((c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
        AND c1.concept_class_id = 'Ingredient'
        AND c1.standard_concept = 'S')
    OR (c1.vocabulary_id = 'RxNorm'
        AND c1.concept_class_id = 'Precise Ingredient')
    OR (c1.vocabulary_id = 'ATC'
        AND c1.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
        AND c1.standard_concept = 'C'))

  AND c2.domain_id = 'Drug'
  AND ((c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
        AND c2.concept_class_id = 'Ingredient'
        AND c2.standard_concept = 'S')
    OR (c2.vocabulary_id = 'RxNorm'
        AND c2.concept_class_id = 'Precise Ingredient')
    OR (c2.vocabulary_id = 'ATC'
        AND c2.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
        AND c2.standard_concept = 'C'))