SELECT EXTRACT(YEAR FROM CURRENT_DATE()) - p.age AS age, p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.year_of_birth AS year_of_birth FROM `broad-tanagra-dev.cmssynpuf_index_082523`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.cmssynpuf_index_082523`.ingredient_person_occurrence_ingredient_occurrence_person_idpairs AS i WHERE i.id_ingredient_occurrence IN (SELECT i.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.ingredient_occurrence AS i WHERE i.id IN (SELECT i.id_ingredient_occurrence FROM `broad-tanagra-dev.cmssynpuf_index_082523`.ingredient_person_occurrence_ingredient_occurrence_ingredient_idpairs AS i WHERE i.id_ingredient IN (SELECT i.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.ingredient AS i WHERE i.id = 1177480)))) LIMIT 30
