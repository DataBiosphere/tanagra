SELECT c.standard_concept AS standard_concept,
 c.vocabulary_id AS vocabulary, v.vocabulary_name AS t_display_vocabulary,
 c.concept_name AS name,
 c.concept_code AS concept_code,
 c.concept_id AS id

 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c
 JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary AS v ON v.vocabulary_id = c.vocabulary_id

 WHERE c.domain_id = 'Condition' AND c.valid_end_date > '2022-01-01'
