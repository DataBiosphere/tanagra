SELECT condition_alias.concept_id AS concept_id, condition_alias.concept_name AS concept_name, condition_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = condition_alias.vocabulary_id) AS vocabulary_name, condition_alias.standard_concept AS standard_concept, condition_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Condition' AND valid_end_date > '2022-01-01') AS condition_alias WHERE (SELECT condition_node_path_2.path FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.condition_node_path_2 WHERE condition_node_path_2.node = condition_alias.concept_id) = ''
