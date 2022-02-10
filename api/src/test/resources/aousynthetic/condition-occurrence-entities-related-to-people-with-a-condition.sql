SELECT co.condition_occurrence_id AS condition_occurrence_id, co.person_id AS person_id, co.condition_concept_id AS condition_concept_id, (SELECT concept.concept_name FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE concept.concept_id = co.condition_concept_id) AS condition_name, (SELECT concept.standard_concept FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE concept.concept_id = co.condition_concept_id) AS condition_standard, (SELECT concept.concept_code FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE concept.concept_id = co.condition_concept_id) AS condition_concept_code FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.condition_occurrence AS co WHERE co.person_id IN (SELECT p.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS p WHERE p.person_id IN (SELECT condition_person1329273297.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.condition_occurrence AS condition_person1329273297 WHERE condition_person1329273297.condition_concept_id IN (SELECT c.concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Condition' AND valid_end_date > '2022-01-01') AS c WHERE c.concept_id = 439676)))
