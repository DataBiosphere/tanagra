SELECT p.id AS id FROM `verily-tanagra-dev.sd20230328_index_072623`.person AS p WHERE p.id IN (SELECT i.id_person FROM `verily-tanagra-dev.sd20230328_index_072623`.idpairs_snp_person AS i WHERE i.id_snp IN (SELECT s.id FROM `verily-tanagra-dev.sd20230328_index_072623`.snp AS s WHERE s.name = 'RS12925749')) LIMIT 30
