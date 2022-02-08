-- BuildPathsForHierarchy all-nodes input query for procedures. This must match the entityMapping for the procedure entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Procedure'
AND c.valid_end_date > DATE('2022-01-01')
;
