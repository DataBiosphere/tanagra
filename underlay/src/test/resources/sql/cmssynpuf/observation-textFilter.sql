
    SELECT
        t.concept_code AS concept_code,
        t.id AS id,
        t.name AS name,
        t.standard_concept AS standard_concept,
        t.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.T_ENT_observation AS t 
    WHERE
        REGEXP_CONTAINS(UPPER(t.T_TXT), UPPER('smoke')) LIMIT 30