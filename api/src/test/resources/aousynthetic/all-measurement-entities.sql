SELECT measurement_alias.concept_id AS concept_id, measurement_alias.concept_name AS concept_name, measurement_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = measurement_alias.vocabulary_id) AS vocabulary_name, measurement_alias.standard_concept AS standard_concept, measurement_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Measurement') AS measurement_alias WHERE TRUE
