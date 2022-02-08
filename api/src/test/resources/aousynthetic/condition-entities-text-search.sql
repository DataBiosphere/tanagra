SELECT condition_alias.concept_id AS concept_id, condition_alias.concept_name AS concept_name, condition_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = condition_alias.vocabulary_id) AS vocabulary_name, condition_alias.standard_concept AS standard_concept, condition_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Condition' AND valid_end_date > '2022-01-01') AS condition_alias WHERE condition_alias.concept_id IN (SELECT concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept_synonym WHERE CONTAINS_SUBSTR(concept_synonym_name, 'sense of smell absent'))
