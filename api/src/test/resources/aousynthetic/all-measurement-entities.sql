SELECT measurement_alias.concept_id AS concept_id, measurement_alias.concept_name AS concept_name, measurement_alias.vocabulary_id AS vocabulary_id, (SELECT vocabulary.vocabulary_name FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary WHERE vocabulary.vocabulary_id = measurement_alias.vocabulary_id) AS vocabulary_name, measurement_alias.standard_concept AS standard_concept, measurement_alias.concept_code AS concept_code FROM (SELECT * FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE valid_end_date > '2022-01-01' AND ((domain_id = 'Measurement' AND vocabulary_id = 'SNOMED') OR (vocabulary_id = 'LOINC' AND (concept_class_id = 'LOINC Hierarchy' OR concept_class_id = 'LOINC Component' OR concept_class_id = 'Lab Test')))) AS measurement_alias WHERE TRUE
