SELECT g.id AS id, g.name AS name, (g.t_standard_path IS NOT NULL) AS t_standard_is_member, (g.t_standard_path IS NOT NULL AND g.t_standard_path='') AS t_standard_is_root, g.t_standard_num_children AS t_standard_num_children, g.t_standard_path AS t_standard_path FROM `verily-tanagra-dev.sdstatic_index_072623`.genotyping AS g WHERE g.id IN (SELECT g.child FROM `verily-tanagra-dev.sdstatic_index_072623`.genotyping_standard_childParent AS g WHERE g.parent = 101) LIMIT 30
