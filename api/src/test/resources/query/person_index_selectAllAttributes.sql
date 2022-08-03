SELECT p.gender AS gender, p.t_display_gender AS t_display_gender,
 p.race AS race, p.t_display_race AS t_display_race,
 p.ethnicity AS ethnicity, p.t_display_ethnicity AS t_display_ethnicity,
 p.id AS id,
 p.sex_at_birth AS sex_at_birth, p.t_display_sex_at_birth AS t_display_sex_at_birth,
 p.year_of_birth AS year_of_birth

 FROM `verily-tanagra-dev.aou_synthetic_SR2019q4r4_indexes`.person AS p
