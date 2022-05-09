-- BuildPathsForHierarchy parent-child relationships input query for measurements.
SELECT
  cr.concept_id_1 AS parent,
  cr.concept_id_2 AS child,
FROM `victr-tanagra-test.sd_static.concept_relationship` cr
JOIN `victr-tanagra-test.sd_static.concept` c1  ON c1.concept_id = cr.concept_id_1
JOIN `victr-tanagra-test.sd_static.concept` c2  ON c2.concept_id = cr.concept_id_2
WHERE
  cr.relationship_id = 'Subsumes'
  AND c1.vocabulary_id = c2.vocabulary_id
  AND ((c1.vocabulary_id = 'SNOMED' AND c1.domain_id = c2.domain_id AND c1.domain_id = 'Measurement')
      OR (c1.vocabulary_id = 'LOINC'
          AND c1.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
          AND c2.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))
  AND c1.valid_end_date > DATE('2022-01-01')
  AND c2.valid_end_date > DATE('2022-01-01')
;
