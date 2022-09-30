SELECT c.concept_code AS concept_code, c.concept_id AS id, c.concept_name AS name, c.standard_concept AS standard_concept, (CASE WHEN c.standard_concept IS NULL THEN 'Source' WHEN c.standard_concept = 'S' THEN 'Standard' ELSE 'Unknown' END) AS t_display_standard_concept, v.vocabulary_name AS t_display_vocabulary, c.vocabulary_id AS vocabulary FROM (SELECT c.* FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c WHERE (c.domain_id = 'Condition' AND c.valid_end_date > '2022-01-01')) AS c LEFT JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.vocabulary AS v ON v.vocabulary_id = c.vocabulary_id