SELECT p.concept_code AS concept_code, p.id AS id, p.name AS name, p.standard_concept AS standard_concept, p.t_display_standard_concept AS t_display_standard_concept, p.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index_072623`.procedure AS p WHERE CONTAINS_SUBSTR(p.text, 'mammogram') LIMIT 30
