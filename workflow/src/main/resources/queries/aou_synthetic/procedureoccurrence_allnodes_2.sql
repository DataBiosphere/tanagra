-- PrecomputeCounts all-auxiliary-nodes input query for procedures. This must match the entityMapping for the procedure occurrence entity in the aou_synthetic underlay.
SELECT
  po.procedure_concept_id AS node,
  po.person_id AS secondary
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.procedure_occurrence` po
;
