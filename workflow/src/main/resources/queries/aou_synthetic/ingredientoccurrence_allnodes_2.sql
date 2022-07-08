-- PrecomputeCounts all-auxiliary-nodes input query for ingredients. This must match the entityMapping for the ingredient occurrence entity in the aou_synthetic underlay.
SELECT
  de.drug_concept_id AS node,
  de.person_id AS what_to_count
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.drug_exposure` de
;
