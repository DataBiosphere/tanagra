SELECT c.concept_code AS concept_code, c.id AS id, c.name AS name, c.standard_concept AS standard_concept, c.t_display_standard_concept AS t_display_standard_concept, c.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index_072623`.condition AS c WHERE CONTAINS_SUBSTR(c.text, 'sense of smell absent') LIMIT 30
