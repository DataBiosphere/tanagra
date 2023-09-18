SELECT v.id AS id, v.name AS name FROM `broad-tanagra-dev.aousynthetic_index_082523`.visit AS v WHERE REGEXP_CONTAINS(UPPER(v.text), UPPER('ambul')) LIMIT 30
