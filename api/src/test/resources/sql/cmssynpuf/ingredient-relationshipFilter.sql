SELECT i.concept_code AS concept_code, i.id AS id, i.name AS name, i.standard_concept AS standard_concept, i.t_display_standard_concept AS t_display_standard_concept, i.t_display_vocabulary AS t_display_vocabulary, i.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index`.ingredient AS i WHERE i.id IN (SELECT i.id_ingredient FROM `broad-tanagra-dev.cmssynpuf_index`.idpairs_brand_ingredient AS i WHERE i.id_brand IN (SELECT b.id FROM `broad-tanagra-dev.cmssynpuf_index`.brand AS b WHERE b.id = 19082059)) LIMIT 30
