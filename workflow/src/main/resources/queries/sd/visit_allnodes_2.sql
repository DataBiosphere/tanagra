-- BuildPathsForHierarchy all-nodes input query for visits. This must match the entityMapping for the visit entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Visit'
AND c.standard_concept = 'S'
;
