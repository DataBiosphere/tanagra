SELECT p.gender AS gender, p.race AS race, COUNT(p.id) AS t_count, p.t_display_gender AS t_display_gender, p.t_display_race AS t_display_race FROM `verily-tanagra-dev.sd20230328_index_083023`.person AS p WHERE p.id IN (SELECT s.id_person FROM `verily-tanagra-dev.sd20230328_index_083023`.snp_person_snp_person_idpairs AS s WHERE s.id_snp IN (SELECT s.id FROM `verily-tanagra-dev.sd20230328_index_083023`.snp AS s WHERE s.name = 'RS12925749')) GROUP BY gender, t_display_gender, race, t_display_race ORDER BY gender ASC, t_display_gender ASC, race ASC, t_display_race ASC
