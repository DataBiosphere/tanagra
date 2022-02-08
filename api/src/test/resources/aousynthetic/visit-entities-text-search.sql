SELECT visit_alias.concept_id AS concept_id, visit_alias.concept_name AS concept_name FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE vocabulary_id = 'Visit') AS visit_alias WHERE visit_alias.concept_id IN (SELECT concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE CONTAINS_SUBSTR(concept_name, 'ambul'))
