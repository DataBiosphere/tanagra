SELECT observation_alias.concept_id AS concept_id, observation_alias.concept_name AS concept_name, observation_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = observation_alias.vocabulary_id) AS vocabulary_name, observation_alias.standard_concept AS standard_concept, observation_alias.concept_code AS concept_code FROM (SELECT * FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Observation' AND standard_concept = 'S' AND vocabulary_id != 'PPI' AND concept_class_id != 'Survey') AS observation_alias WHERE observation_alias.concept_id IN (SELECT node FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.observation_text_search_2 WHERE CONTAINS_SUBSTR(text, 'smoke'))
