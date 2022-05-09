-- BuildPathsForHierarchy root-node-filter input query for measurements. Root nodes that are not included in this filter, will be removed from the hierarchy.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE ((c.domain_id = 'Measurement' AND c.vocabulary_id = 'SNOMED')
        OR (c.vocabulary_id = 'LOINC' AND c.concept_id = 36206173))
AND c.valid_end_date > DATE('2022-01-01')
;
