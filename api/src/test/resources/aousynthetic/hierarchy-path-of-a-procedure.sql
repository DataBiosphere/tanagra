SELECT (SELECT concept_node_path_2.path FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.concept_node_path_2 WHERE concept_node_path_2.node = procedure_alias.concept_id) AS t_path_concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Procedure') AS procedure_alias WHERE procedure_alias.concept_id = 4198190
