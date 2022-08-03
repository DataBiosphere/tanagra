SELECT c.concept_id AS id
 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c
 WHERE c.domain_id = 'Condition' AND c.valid_end_date > '2022-01-01'

 UNION ALL

 SELECT c.concept_name AS name
 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c
 WHERE c.domain_id = 'Condition' AND c.valid_end_date > '2022-01-01'
