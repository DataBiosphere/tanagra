SELECT v.id AS id, v.name AS name FROM `broad-tanagra-dev.aousynthetic_index_011523`.visit AS v WHERE CONTAINS_SUBSTR(v.text, 'ambul') LIMIT 30
