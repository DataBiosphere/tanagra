SELECT p.ethnicity AS ethnicity, p.gender AS gender, p.id AS id, p.race AS race, p.t_display_ethnicity AS t_display_ethnicity, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race, p.year_of_birth AS year_of_birth FROM `verily-tanagra-dev.sdstatic_index_011923`.person AS p WHERE p.id IN (SELECT i.id_person FROM `verily-tanagra-dev.sdstatic_index_011923`.idpairs_note_occurrence_person AS i WHERE i.id_note_occurrence IN (SELECT n.id FROM `verily-tanagra-dev.sdstatic_index_011923`.note_occurrence AS n WHERE n.id IN (SELECT i.id_note_occurrence FROM `verily-tanagra-dev.sdstatic_index_011923`.idpairs_note_occurrence_note AS i WHERE i.id_note IN (SELECT n.id FROM `verily-tanagra-dev.sdstatic_index_011923`.note AS n WHERE n.id = 44814638)))) LIMIT 30
