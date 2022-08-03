SELECT p.gender_concept_id AS gender, c.concept_name AS t_display_gender,
 p.race_concept_id AS race, c0.concept_name AS t_display_race,
 p.ethnicity_concept_id AS ethnicity, c1.concept_name AS t_display_ethnicity,
 p.person_id AS id,
 p.sex_at_birth_concept_id AS sex_at_birth, c2.concept_name AS t_display_sex_at_birth,
 p.year_of_birth AS year_of_birth

 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS p
 JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c ON c.concept_id = p.gender_concept_id
 JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c0 ON c0.concept_id = p.race_concept_id
 JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c1 ON c1.concept_id = p.ethnicity_concept_id
 JOIN `verily-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c2 ON c2.concept_id = p.sex_at_birth_concept_id
