SELECT procedure_alias.concept_id AS concept_id, procedure_alias.concept_name AS concept_name, procedure_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = procedure_alias.vocabulary_id) AS vocabulary_name, procedure_alias.standard_concept AS standard_concept, procedure_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Procedure' AND valid_end_date > '2022-01-01') AS procedure_alias WHERE procedure_alias.concept_id IN (SELECT child FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.procedure_parent_child_2 WHERE parent = 4179181)
