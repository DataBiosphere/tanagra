-- BuildPathsForHierarchy all-nodes input query for observations. This must match the entityMapping for the obesrvation entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Observation'
AND c.standard_concept = 'S'
AND c.vocabulary_id != 'PPI'
AND c.concept_class_id != 'Survey'
;
