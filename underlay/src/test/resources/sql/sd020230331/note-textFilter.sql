
    SELECT
        e.concept_code AS concept_code,
        e.id AS id,
        e.name AS name,
        e.standard_concept AS standard_concept,
        e.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_note AS e 
    WHERE
        REGEXP_CONTAINS(UPPER(e.T_TXT), UPPER('admis')) LIMIT 30
