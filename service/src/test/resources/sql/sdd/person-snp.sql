SELECT p.id AS id FROM `verily-tanagra-dev.sdstatic_index_011923`.person AS p WHERE p.id IN (SELECT i.id_person FROM `verily-tanagra-dev.sdstatic_index_011923`.idpairs_snp_person AS i WHERE i.id_snp IN (SELECT s.id FROM `verily-tanagra-dev.sdstatic_index_011923`.snp AS s WHERE s.id = 'RS12925749')) LIMIT 30
