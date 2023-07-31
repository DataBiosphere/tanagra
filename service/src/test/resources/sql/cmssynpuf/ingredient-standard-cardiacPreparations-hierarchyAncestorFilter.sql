SELECT i.concept_code AS concept_code, i.id AS id, i.name AS name, i.standard_concept AS standard_concept, i.t_display_standard_concept AS t_display_standard_concept, (i.t_standard_path IS NOT NULL) AS t_standard_is_member, (i.t_standard_path IS NOT NULL AND i.t_standard_path='') AS t_standard_is_root, i.t_standard_num_children AS t_standard_num_children, i.t_standard_path AS t_standard_path, i.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index_072623`.ingredient AS i WHERE (i.id IN (SELECT i.descendant FROM `broad-tanagra-dev.cmssynpuf_index_072623`.ingredient_standard_ancestorDescendant AS i WHERE i.ancestor = 21600360) OR i.id = 21600360) LIMIT 30
