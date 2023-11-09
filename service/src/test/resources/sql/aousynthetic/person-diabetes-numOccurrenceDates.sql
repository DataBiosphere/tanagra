SELECT p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.sex_at_birth AS sex_at_birth, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.t_display_sex_at_birth AS t_display_sex_at_birth, p.year_of_birth AS year_of_birth FROM `broad-tanagra-dev.aousynthetic_index_082523`.person AS p WHERE p.id IN (SELECT x.person_id FROM (SELECT c.person_id, c.condition FROM `broad-tanagra-dev.aousynthetic_index_082523`.condition_occurrence AS c WHERE c.condition IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_082523`.condition AS c WHERE c.id = 201826) GROUP BY c.person_id, c.condition) AS x GROUP BY x.person_id HAVING COUNT(*) > 1) LIMIT 30