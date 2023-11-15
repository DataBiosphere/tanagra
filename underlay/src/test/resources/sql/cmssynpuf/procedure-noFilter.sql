
    SELECT
        e.concept_code AS concept_code,
        e.id AS id,
        e.name AS name,
        e.standard_concept AS standard_concept,
        e.vocabulary AS vocabulary 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_procedure AS e LIMIT 30
