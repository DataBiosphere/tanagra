SELECT ingredient_alias.concept_id AS concept_id, ingredient_alias.concept_name AS concept_name, ingredient_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = ingredient_alias.vocabulary_id) AS vocabulary_name, ingredient_alias.standard_concept AS standard_concept, ingredient_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Drug' AND concept_class_id != 'Brand Name' AND concept_class_id != 'Branded Drug' AND concept_class_id != 'Branded Drug Comp' AND concept_class_id != 'Branded Drug Form' AND concept_class_id != 'Branded Dose Group') AS ingredient_alias WHERE (SELECT concept_node_path_2.path FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.concept_node_path_2 WHERE concept_node_path_2.node = ingredient_alias.concept_id) IS NULL
