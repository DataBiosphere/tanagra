SELECT p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.sex_at_birth AS sex_at_birth, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.t_display_sex_at_birth AS t_display_sex_at_birth, p.year_of_birth AS year_of_birth FROM `broad-tanagra-dev.aousynthetic_index_011523`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.aousynthetic_index_011523`.idpairs_condition_occurrence_person AS i WHERE i.id_condition_occurrence IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_011523`.condition_occurrence AS c WHERE c.id IN (SELECT i.id_condition_occurrence FROM `broad-tanagra-dev.aousynthetic_index_011523`.idpairs_condition_occurrence_condition AS i WHERE i.id_condition IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_011523`.condition AS c WHERE c.id = 201826)))) LIMIT 30
