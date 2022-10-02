SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `victr-tanagra-test.sd_static.concept_relationship` cr
JOIN `victr-tanagra-test.sd_static.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `victr-tanagra-test.sd_static.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id IN ('Has form', 'RxNorm - ATC', 'RxNorm - ATC name', 'Mapped from', 'Subsumes')
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Drug'
  AND c1.concept_id != c2.concept_id

  AND c1.vocabulary_id IN ('RxNorm', 'RxNorm Extension', 'ATC')
  AND c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension', 'ATC')

UNION ALL

SELECT
  ca.ancestor_concept_id AS parent,
  ca.descendant_concept_id AS child,
FROM `victr-tanagra-test.sd_static.concept_ancestor` ca
JOIN `victr-tanagra-test.sd_static.concept` c1  ON c1.concept_id = ca.ancestor_concept_id
JOIN `victr-tanagra-test.sd_static.concept` c2  ON c2.concept_id = ca.descendant_concept_id
WHERE
  ca.min_levels_of_separation = 1
  AND ca.max_levels_of_separation = 1
  AND c1.domain_id = c2.domain_id
  AND c2.domain_id = 'Drug'

  AND c1.vocabulary_id IN ('ATC')
  AND c1.concept_class_id = 'ATC 4th'
  AND c2.vocabulary_id IN ('RxNorm', 'RxNorm Extension')