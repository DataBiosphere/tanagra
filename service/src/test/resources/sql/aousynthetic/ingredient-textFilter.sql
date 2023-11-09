SELECT i.concept_code AS concept_code, i.id AS id, i.name AS name, i.standard_concept AS standard_concept, i.t_display_standard_concept AS t_display_standard_concept, i.t_display_vocabulary AS t_display_vocabulary, i.vocabulary AS vocabulary FROM `broad-tanagra-dev.aousynthetic_index_082523`.ingredient AS i WHERE REGEXP_CONTAINS(UPPER(i.text), UPPER('alcohol')) LIMIT 30