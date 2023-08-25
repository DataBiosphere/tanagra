SELECT i.concept_code AS concept_code, i.id AS id, i.name AS name, i.standard_concept AS standard_concept, i.t_display_standard_concept AS t_display_standard_concept, i.t_display_vocabulary AS t_display_vocabulary, (i.t_standard_path IS NOT NULL) AS t_standard_is_member, (i.t_standard_path IS NOT NULL AND i.t_standard_path='') AS t_standard_is_root, i.t_standard_num_children AS t_standard_num_children, i.t_standard_path AS t_standard_path, i.vocabulary AS vocabulary FROM `broad-tanagra-dev.aousynthetic_index_082523`.ingredient AS i WHERE i.t_standard_path = '' LIMIT 30
