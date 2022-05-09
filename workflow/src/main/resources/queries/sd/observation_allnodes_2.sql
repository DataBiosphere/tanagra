-- BuildPathsForHierarchy all-nodes input query for observations. This must match the entityMapping for the obesrvation entity in the sd underlay.
SELECT
  c.concept_id AS node
FROM  `victr-tanagra-test.sd_static.concept` c
WHERE c.domain_id = 'Observation'
AND c.standard_concept = 'S'
AND c.vocabulary_id != 'PPI'
AND c.concept_class_id != 'Survey'
;
