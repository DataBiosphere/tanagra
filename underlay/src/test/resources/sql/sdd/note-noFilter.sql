
    SELECT
        t.concept_code AS concept_code,
        t.id AS id,
        t.name AS name,
        t.standard_concept AS standard_concept,
        t.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_note AS t LIMIT 30
