SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept_relationship` cr
JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id IN ('Has form', 'RxNorm - ATC', 'RxNorm - ATC name', 'Mapped from', 'Subsumes')
  AND c1.concept_id != c2.concept_id

  AND ((c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
          AND c1.concept_class_id = 'Ingredient'
          AND c1.standard_concept = 'S')
      OR (c1.vocabulary_id = 'RxNorm'
          AND c1.concept_class_id = 'Precise Ingredient')
      OR (c1.vocabulary_id = 'ATC'
          AND c1.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
          AND c1.standard_concept = 'C'))

  AND ((c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
          AND c2.concept_class_id = 'Ingredient'
          AND c2.standard_concept = 'S')
      OR (c2.vocabulary_id = 'RxNorm'
          AND c2.concept_class_id = 'Precise Ingredient')
      OR (c2.vocabulary_id = 'ATC'
          AND c2.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
          AND c2.standard_concept = 'C'))

UNION ALL

SELECT
  ca.ancestor_concept_id AS parent,
  ca.descendant_concept_id AS child,
FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept_ancestor` ca
JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c1  ON c1.concept_id = ca.ancestor_concept_id
JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c2  ON c2.concept_id = ca.descendant_concept_id
WHERE
  ca.min_levels_of_separation = 1
  AND ca.max_levels_of_separation = 1

  AND ((c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
          AND c1.concept_class_id = 'Ingredient'
          AND c1.standard_concept = 'S')
      OR (c1.vocabulary_id = 'RxNorm'
          AND c1.concept_class_id = 'Precise Ingredient')
      OR (c1.vocabulary_id = 'ATC'
          AND c1.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
          AND c1.standard_concept = 'C'))

  AND ((c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')
          AND c2.concept_class_id = 'Ingredient'
          AND c2.standard_concept = 'S')
      OR (c2.vocabulary_id = 'RxNorm'
          AND c2.concept_class_id = 'Precise Ingredient')
      OR (c2.vocabulary_id = 'ATC'
          AND c2.concept_class_id IN ('ATC 1st', 'ATC 2nd', 'ATC 3rd', 'ATC 4th', 'ATC 5th')
          AND c2.standard_concept = 'C'))