SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept_relationship` cr
JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id IN ('Has form', 'RxNorm - ATC', 'RxNorm - ATC name', 'Mapped from', 'Subsumes')
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Drug'
  AND c1.concept_id != c2.concept_id

  AND c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension', 'ATC')
  AND c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension', 'ATC')

  AND c1.valid_end_date > DATE('2022-01-01')
  AND c2.valid_end_date > DATE('2022-01-01')

UNION ALL

SELECT
  ca.ancestor_concept_id AS parent,
  ca.descendant_concept_id AS child,
FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept_ancestor` ca
JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c1  ON c1.concept_id = ca.ancestor_concept_id
JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c2  ON c2.concept_id = ca.descendant_concept_id
WHERE
  ca.min_levels_of_separation = 1
  AND ca.max_levels_of_separation = 1
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Drug'

  AND c1.vocabulary_id IN ('ATC')
  AND c1.concept_class_id = 'ATC 4th'
  AND c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')

  AND c1.valid_end_date > DATE('2022-01-01')
  AND c2.valid_end_date > DATE('2022-01-01')