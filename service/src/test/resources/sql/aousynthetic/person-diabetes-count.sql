SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `broad-tanagra-dev.aousynthetic_index_031323`.person AS p WHERE p.id IN (SELECT i.id_person FROM `broad-tanagra-dev.aousynthetic_index_031323`.idpairs_condition_occurrence_person AS i WHERE i.id_condition_occurrence IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_031323`.condition_occurrence AS c WHERE c.id IN (SELECT i.id_condition_occurrence FROM `broad-tanagra-dev.aousynthetic_index_031323`.idpairs_condition_occurrence_condition AS i WHERE i.id_condition IN (SELECT c.id FROM `broad-tanagra-dev.aousynthetic_index_031323`.condition AS c WHERE c.id = 201826)))) GROUP BY p.gender, p.t_display_gender, p.race, p.t_display_race ORDER BY p.gender ASC, p.t_display_gender ASC, p.race ASC, p.t_display_race ASC
