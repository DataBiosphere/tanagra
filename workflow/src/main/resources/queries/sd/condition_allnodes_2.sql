-- BuildPathsForHierarchy all-nodes input query for conditions. This must match the entityMapping for the condition entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Condition'
AND c.valid_end_date > DATE('2022-01-01')
;
