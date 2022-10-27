SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `broad-tanagra-dev.aousynthetic_index`.person AS p GROUP BY p.gender, p.t_display_gender, p.race, p.t_display_race ORDER BY p.gender, p.t_display_gender, p.race, p.t_display_race ASC
