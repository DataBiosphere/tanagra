SELECT device_alias.concept_id AS concept_id, device_alias.concept_name AS concept_name, device_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = device_alias.vocabulary_id) AS vocabulary_name, device_alias.standard_concept AS standard_concept, device_alias.concept_code AS concept_code FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Device') AS device_alias WHERE device_alias.concept_id IN (SELECT concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept_synonym WHERE CONTAINS_SUBSTR(concept_synonym_name, 'hearing aid'))
