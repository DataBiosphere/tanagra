-- BuildPathsForHierarchy all-nodes input query for measurements. This must match the entityMapping for the measurement entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')))
AND c.valid_end_date > DATE('2022-01-01')
;
