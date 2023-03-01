SELECT p.concept_code AS concept_code, p.id AS id, p.name AS name, p.standard_concept AS standard_concept, p.t_display_standard_concept AS t_display_standard_concept, p.t_display_vocabulary AS t_display_vocabulary, (p.t_standard_path IS NOT NULL) AS t_standard_is_member, (p.t_standard_path IS NOT NULL AND p.t_standard_path='') AS t_standard_is_root, p.t_standard_num_children AS t_standard_num_children, p.t_standard_path AS t_standard_path, p.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index_030123`.procedure AS p WHERE p.t_standard_path IS NOT NULL LIMIT 30
