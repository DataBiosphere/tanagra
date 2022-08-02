SELECT p.gender_concept_id AS gender, c.concept_name AS t_display_gender, p.person_id AS id, p.year_of_birth AS year_of_birth

 FROM `verily-tanagra-dev.aou_synthetic`.person AS p

 JOIN `verily-tanagra-dev.aou_synthetic`.concept AS c ON c.concept_id = p.gender_concept_id