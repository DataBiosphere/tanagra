-- BuildPathsForHierarchy all-nodes input query for devices. This must match the entityMapping for the device entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Device'
AND c.standard_concept = 'S'
;
