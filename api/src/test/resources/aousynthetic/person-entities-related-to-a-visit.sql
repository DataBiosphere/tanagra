SELECT person_alias.person_id AS person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS person_alias WHERE person_alias.person_id IN (SELECT visit_occurrence_alias1.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.visit_occurrence AS visit_occurrence_alias1 WHERE visit_occurrence_alias1.visit_concept_id IN (SELECT visit_alias.concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Visit' AND standard_concept = 'S') AS visit_alias WHERE visit_alias.concept_id = 9202))
