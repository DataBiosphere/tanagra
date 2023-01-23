SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `verily-tanagra-dev.sdstatic_index_011923`.person AS p WHERE p.id IN (SELECT i.id_person FROM `verily-tanagra-dev.sdstatic_index_011923`.idpairs_note_occurrence_person AS i WHERE i.id_note_occurrence IN (SELECT n.id FROM `verily-tanagra-dev.sdstatic_index_011923`.note_occurrence AS n WHERE n.id IN (SELECT i.id_note_occurrence FROM `verily-tanagra-dev.sdstatic_index_011923`.idpairs_note_occurrence_note AS i WHERE i.id_note IN (SELECT n.id FROM `verily-tanagra-dev.sdstatic_index_011923`.note AS n WHERE n.id = 44814638)))) GROUP BY p.gender, p.t_display_gender, p.race, p.t_display_race ORDER BY p.gender ASC, p.t_display_gender ASC, p.race ASC, p.t_display_race ASC
