SELECT o.concept_code AS concept_code, o.id AS id, o.name AS name, o.standard_concept AS standard_concept, o.t_display_standard_concept AS t_display_standard_concept, o.t_display_vocabulary AS t_display_vocabulary, o.vocabulary AS vocabulary FROM `broad-tanagra-dev.cmssynpuf_index`.observation AS o WHERE CONTAINS_SUBSTR(o.text, 'smoke') LIMIT 30
