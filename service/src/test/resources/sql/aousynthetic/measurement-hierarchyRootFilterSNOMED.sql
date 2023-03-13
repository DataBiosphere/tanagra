SELECT m.concept_code AS concept_code, m.id AS id, m.name AS name, m.standard_concept AS standard_concept, m.t_display_standard_concept AS t_display_standard_concept, m.t_display_vocabulary AS t_display_vocabulary, (m.t_standard_path IS NOT NULL) AS t_standard_is_member, (m.t_standard_path IS NOT NULL AND m.t_standard_path='') AS t_standard_is_root, m.t_standard_num_children AS t_standard_num_children, m.t_standard_path AS t_standard_path, m.vocabulary AS vocabulary FROM `broad-tanagra-dev.aousynthetic_index_031323`.measurement AS m WHERE (m.t_standard_path = '' AND m.vocabulary = 'SNOMED') LIMIT 30
