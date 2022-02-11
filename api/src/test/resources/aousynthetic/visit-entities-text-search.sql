SELECT visit_alias.concept_id AS concept_id, visit_alias.concept_name AS concept_name FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Visit' AND standard_concept = 'S') AS visit_alias WHERE visit_alias.concept_id IN (SELECT node FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.visit_text_search_2 WHERE CONTAINS_SUBSTR(text, 'ambul'))
