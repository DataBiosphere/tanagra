SELECT b.concept_code AS concept_code, b.id AS id, b.name AS name, b.standard_concept AS standard_concept, b.t_display_standard_concept AS t_display_standard_concept FROM `broad-tanagra-dev.cmssynpuf_index_082523`.brand AS b WHERE REGEXP_CONTAINS(UPPER(b.text), UPPER('paracetamol')) LIMIT 30