-- PrecomputeCounts all-auxiliary-nodes input query for devices. This must match the entityMapping for the device occurrence entity in the aou_synthetic underlay.
SELECT
  de.device_concept_id AS node,
  de.person_id AS what_to_count
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.device_exposure` de
;
