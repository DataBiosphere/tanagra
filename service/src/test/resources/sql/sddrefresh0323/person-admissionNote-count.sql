SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `verily-tanagra-dev.sd20230331_index_092123`.person AS p WHERE p.id IN (SELECT n.person_id FROM `verily-tanagra-dev.sd20230331_index_092123`.note_occurrence AS n WHERE n.note IN (SELECT n.id FROM `verily-tanagra-dev.sd20230331_index_092123`.note AS n WHERE n.id = 44814638)) GROUP BY gender, t_display_gender, race, t_display_race ORDER BY gender ASC, t_display_gender ASC, race ASC, t_display_race ASC
