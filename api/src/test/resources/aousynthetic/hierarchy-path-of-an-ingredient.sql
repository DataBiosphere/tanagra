SELECT (SELECT concept_node_path_2.path FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.concept_node_path_2 WHERE concept_node_path_2.node = ingredient_alias.concept_id) AS t_path_concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Drug' AND concept_class_id != 'Brand Name' AND concept_class_id != 'Branded Drug' AND concept_class_id != 'Branded Drug Comp' AND concept_class_id != 'Branded Drug Form' AND concept_class_id != 'Branded Dose Group') AS ingredient_alias WHERE ingredient_alias.concept_id = 1784444
