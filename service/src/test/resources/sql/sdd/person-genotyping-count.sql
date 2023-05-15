SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `verily-tanagra-dev.sdstatic_index_051023`.person AS p WHERE p.id IN (SELECT i.id_person FROM `verily-tanagra-dev.sdstatic_index_051023`.idpairs_genotyping_person AS i WHERE i.id_genotyping IN (SELECT g.id FROM `verily-tanagra-dev.sdstatic_index_051023`.genotyping AS g WHERE g.name = 'Illumina 5M')) GROUP BY gender, t_display_gender, race, t_display_race ORDER BY gender ASC, t_display_gender ASC, race ASC, t_display_race ASC
