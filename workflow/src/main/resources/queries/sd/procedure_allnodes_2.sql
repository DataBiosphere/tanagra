-- BuildPathsForHierarchy all-nodes input query for procedures. This must match the entityMapping for the procedure entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Procedure'
AND c.valid_end_date > DATE('2022-01-01')
;
