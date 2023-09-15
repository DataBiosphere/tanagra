SELECT p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.sex_at_birth AS sex_at_birth, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.t_display_sex_at_birth AS t_display_sex_at_birth, p.year_of_birth AS year_of_birth FROM `broad-tanagra-dev.aousynthetic_index_082523`.person AS p WHERE p.id IN (SELECT d.person_id FROM `broad-tanagra-dev.aousynthetic_index_082523`.device_occurrence AS d WHERE d.device IN (SELECT d.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.device AS d WHERE d.id = 4038664)) LIMIT 30
