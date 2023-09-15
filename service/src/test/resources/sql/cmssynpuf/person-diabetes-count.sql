SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `broad-tanagra-dev.cmssynpuf_index_082523`.person AS p WHERE p.id IN (SELECT c.person_id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.condition_occurrence AS c WHERE c.condition IN (SELECT c.id FROM `broad-tanagra-dev.cmssynpuf_index_082523`.condition AS c WHERE c.id = 201826)) GROUP BY gender, t_display_gender, race, t_display_race ORDER BY gender ASC, t_display_gender ASC, race ASC, t_display_race ASC
