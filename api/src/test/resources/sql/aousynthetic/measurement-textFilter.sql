SELECT m.concept_code AS concept_code, m.id AS id, m.name AS name, m.standard_concept AS standard_concept, m.t_display_standard_concept AS t_display_standard_concept, m.t_display_vocabulary AS t_display_vocabulary, m.vocabulary AS vocabulary FROM `broad-tanagra-dev.aousynthetic_index`.measurement AS m WHERE CONTAINS_SUBSTR(m.text, 'hematocrit') LIMIT 30
