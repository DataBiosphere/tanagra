SELECT c.concept_code AS concept_code, c.id AS id, c.name AS name, c.standard_concept AS standard_concept, c.t_display_standard_concept AS t_display_standard_concept, (c.t_standard_path IS NOT NULL) AS t_standard_is_member, (c.t_standard_path IS NOT NULL AND c.t_standard_path='') AS t_standard_is_root, c.t_standard_num_children AS t_standard_num_children, c.t_standard_path AS t_standard_path, c.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index_082523`.condition AS c WHERE c.t_standard_path IS NOT NULL LIMIT 30
